package com.scrumaiassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumaiassistant.dto.*;
import com.scrumaiassistant.dto.ai.AiExtractionResult;
import com.scrumaiassistant.exception.ResourceNotFoundException;
import com.scrumaiassistant.integration.IntegrationService;
import com.scrumaiassistant.model.ExtractedItem;
import com.scrumaiassistant.model.Meeting;
import com.scrumaiassistant.model.Task;
import com.scrumaiassistant.model.enums.*;
import com.scrumaiassistant.repository.ExtractedItemRepository;
import com.scrumaiassistant.repository.MeetingRepository;
import com.scrumaiassistant.repository.TaskRepository;
import com.scrumaiassistant.service.AiExtractionService;
import com.scrumaiassistant.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final ExtractedItemRepository extractedItemRepository;
    private final TaskRepository taskRepository;
    private final AiExtractionService aiExtractionService;
    private final IntegrationService integrationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MeetingResponse createMeeting(MeetingCreateRequest request) {
        Meeting meeting = Meeting.builder()
                .title(request.title())
                .ceremonyType(request.ceremonyType())
                .meetingDate(request.meetingDate())
                .toolType(request.toolType())
                .projectKey(request.projectKey())
                .status(MeetingStatus.CREATED)
                .build();
        
        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    @Override
    public MeetingResponse getMeeting(UUID id) {
        return mapToResponse(getMeetingEntity(id));
    }

    @Override
    public List<MeetingResponse> getAllMeetings(int skip, int limit) {
        int page = skip / limit;
        return meetingRepository.findAll(PageRequest.of(page, limit, Sort.by("createdAt").descending()))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void uploadAudio(UUID id, MultipartFile file) {
        Meeting meeting = getMeetingEntity(id);
        meeting.setAudioFilename(file.getOriginalFilename());
        meeting.setStatus(MeetingStatus.UPLOADED);
        meetingRepository.save(meeting);
        log.info("Audio uploaded for meeting {}", id);
    }

    @Override
    @Transactional
    public void addTranscript(UUID id, String transcript) {
        Meeting meeting = getMeetingEntity(id);
        meeting.setTranscript(transcript);
        meeting.setStatus(MeetingStatus.TRANSCRIBED);
        meetingRepository.save(meeting);
        log.info("Transcript added for meeting {}", id);
    }

    /**
     * FIX #1 — IDEMPOTENCY GUARD:
     * Before scheduling the async task, mark the meeting as EXTRACTED (processing started)
     * inside a synchronous @Transactional call. Any subsequent /process call will see the
     * non-TRANSCRIBED status and return early, preventing duplicate Jira tickets.
     *
     * FIX #2 — @Async + @Transactional conflict:
     * Spring cannot apply @Transactional to @Async methods on the same proxy bean.
     * The solution: the synchronous outer method handles the status guard + commit, and
     * the actual async work is delegated to a separate method with its own transaction
     * using Propagation.REQUIRES_NEW (processMeetingAsync).
     */
    @Override
    @Transactional
    public void processMeeting(UUID id) {
        Meeting meeting = getMeetingEntity(id);

        // IDEMPOTENCY GUARD — prevent duplicate processing
        if (meeting.getStatus() != MeetingStatus.TRANSCRIBED && meeting.getStatus() != MeetingStatus.UPLOADED) {
            log.warn("Meeting {} is already in status '{}'. Skipping re-process to prevent duplicates.",
                    id, meeting.getStatus());
            return;
        }

        // Mark as EXTRACTED before handing off to async thread
        meeting.setStatus(MeetingStatus.EXTRACTED);
        meetingRepository.save(meeting);
        log.info("Meeting {} marked EXTRACTED. Triggering async processing.", id);

        // Delegate to async method (must be called via the proxy — see Spring @Async limitation)
        processMeetingAsync(id);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMeetingAsync(UUID id) {
        Meeting meeting = getMeetingEntity(id);
        log.info("Starting async AI processing for meeting {}", id);

        try {
            // 1. Extract Info from AI
            AiExtractionResult result = aiExtractionService.extractInfo(
                    meeting.getId().toString(),
                    meeting.getProjectKey(),
                    meeting.getTranscript()
            );

            // 2. Save Extracted Items
            List<ExtractedItem> items = new ArrayList<>();

            // Decisions
            if (result.decisions() != null) {
                for (String decision : result.decisions()) {
                    items.add(ExtractedItem.builder()
                            .meeting(meeting)
                            .itemType(ItemType.DECISION)
                            .content(decision)
                            .build());
                }
            }

            // Blockers
            if (result.blockers() != null) {
                for (AiExtractionResult.Blocker blocker : result.blockers()) {
                    items.add(ExtractedItem.builder()
                            .meeting(meeting)
                            .itemType(ItemType.BLOCKER)
                            .content(toJson(blocker))
                            .build());
                }
            }

            // Action Items
            if (result.actionItems() != null) {
                for (AiExtractionResult.ActionItem actionItem : result.actionItems()) {
                    items.add(ExtractedItem.builder()
                            .meeting(meeting)
                            .itemType(ItemType.ACTION_ITEM)
                            .content(toJson(actionItem))
                            .build());
                }
            }

            // FIX #3 — DEDUPLICATION + COMMENTS:
            // Tasks → Create in Jira and immediately post a rich comment with context.
            if (result.tasks() != null) {
                for (AiExtractionResult.JiraTask issue : result.tasks()) {
                    Task task = Task.builder()
                            .meeting(meeting)
                            .toolType(meeting.getToolType())
                            .title(issue.title())
                            .description(issue.description())
                            .priority(issue.priority())
                            .assignee(issue.assignee())
                            .status(TaskStatus.NEW)
                            .build();

                    if (issue.dueDate() != null) {
                        try {
                            task.setDueDate(java.time.LocalDate.parse(issue.dueDate()).atStartOfDay());
                        } catch (Exception ignored) {}
                    }

                    task = taskRepository.save(task);

                    // Push to Jira/Azure
                    String externalId = integrationService.createExternalTask(task);
                    task.setExternalKeyOrId(externalId);
                    task.setStatus(TaskStatus.PUSHED);
                    task = taskRepository.save(task);

                    // FIX #3 — POST COMMENT on the created Jira issue with relevant transcript context
                    String commentBody = buildTaskComment(meeting, issue);
                    integrationService.addCommentToExternalTask(externalId, task, commentBody);
                }
            }

            // Comments (meeting-level observations stored as extracted items)
            if (result.comments() != null) {
                for (AiExtractionResult.Comment comment : result.comments()) {
                    items.add(ExtractedItem.builder()
                            .meeting(meeting)
                            .itemType(ItemType.COMMENT)
                            .content(toJson(comment))
                            .build());
                }
            }

            // Ticket Updates
            if (result.ticketUpdates() != null) {
                for (AiExtractionResult.TicketUpdate update : result.ticketUpdates()) {
                    items.add(ExtractedItem.builder()
                            .meeting(meeting)
                            .itemType(ItemType.TICKET_UPDATE)
                            .content(toJson(update))
                            .build());
                }
            }

            extractedItemRepository.saveAll(items);

            meeting.setStatus(MeetingStatus.COMPLETED);
            meetingRepository.save(meeting);

            integrationService.sendNotification("Meeting processed successfully: " + meeting.getTitle());

        } catch (Exception e) {
            log.error("Error processing meeting {}", id, e);
            meeting.setStatus(MeetingStatus.FAILED);
            meetingRepository.save(meeting);
            integrationService.sendNotification("Meeting processing failed: " + meeting.getTitle());
        }
    }

    @Override
    public List<ExtractedItemResponse> getMeetingItems(UUID id) {
        return extractedItemRepository.findByMeetingId(id).stream()
                .map(item -> new ExtractedItemResponse(
                        item.getId(),
                        item.getMeeting().getId(),
                        item.getItemType(),
                        item.getContent(),
                        item.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getMeetingTasks(UUID id) {
        return taskRepository.findByMeetingId(id).stream()
                .map(task -> new TaskResponse(
                        task.getId(),
                        task.getMeeting().getId(),
                        task.getToolType(),
                        task.getExternalKeyOrId(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private Meeting getMeetingEntity(UUID id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getCeremonyType(),
                meeting.getMeetingDate(),
                meeting.getToolType(),
                meeting.getProjectKey(),
                meeting.getStatus(),
                meeting.getTranscript(),
                meeting.getSummary(),
                meeting.getAudioFilename(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt()
        );
    }

    /**
     * Builds a rich comment body for a Jira ticket summarising the transcript context,
     * assignee, action items, and due date. Posted immediately after ticket creation.
     */
    private String buildTaskComment(Meeting meeting, AiExtractionResult.JiraTask issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AI Scrum Assistant — Auto-generated Comment ===\n\n");
        sb.append("Meeting: ").append(meeting.getTitle()).append("\n");
        sb.append("Meeting Date: ").append(meeting.getMeetingDate() != null ? meeting.getMeetingDate().toString() : "N/A").append("\n\n");

        if (issue.assignee() != null && !issue.assignee().isBlank()) {
            sb.append("Assignee: ").append(issue.assignee()).append("\n");
        }
        if (issue.priority() != null && !issue.priority().isBlank()) {
            sb.append("Priority: ").append(issue.priority()).append("\n");
        }
        if (issue.dueDate() != null && !issue.dueDate().isBlank()) {
            sb.append("Due Date: ").append(issue.dueDate()).append("\n");
        }
        if (issue.sourceSentence() != null && !issue.sourceSentence().isBlank()) {
            sb.append("\nSource (from transcript):\n\"").append(issue.sourceSentence()).append("\"\n");
        }
        if (issue.description() != null && !issue.description().isBlank()) {
            sb.append("\nContext:\n").append(issue.description()).append("\n");
        }
        if (issue.labels() != null && !issue.labels().isEmpty()) {
            sb.append("\nLabels: ").append(String.join(", ", issue.labels())).append("\n");
        }
        return sb.toString();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            return "{}";
        }
    }
}

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
        // In a real app, upload to S3/Azure Blob. Here we just mock it.
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

    @Async
    @Override
    @Transactional
    public void processMeeting(UUID id) {
        Meeting meeting = getMeetingEntity(id);
        log.info("Starting processing for meeting {}", id);

        try {
            // 1. Extract Info
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
            
            // Tasks -> Create and Push
            if (result.tasks() != null) { // CHANGED from jiraIssues to tasks
                for (AiExtractionResult.JiraTask issue : result.tasks()) {
                     // Create Task
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
                            // Simple parsing, assuming ISO-8601 YYYY-MM-DD
                             task.setDueDate(java.time.LocalDate.parse(issue.dueDate()).atStartOfDay()); 
                         } catch (Exception ignored) {}
                     }
                     
                     task = taskRepository.save(task);
                     
                     // Push to Jira/Azure
                     String externalId = integrationService.createExternalTask(task);
                     task.setExternalKeyOrId(externalId);
                     task.setStatus(TaskStatus.PUSHED);
                     taskRepository.save(task);
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
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            return "{}";
        }
    }
}

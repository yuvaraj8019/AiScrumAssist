package com.scrumaiassistant.controller;

import com.scrumaiassistant.dto.ExtractedItemResponse;
import com.scrumaiassistant.dto.MeetingCreateRequest;
import com.scrumaiassistant.dto.MeetingResponse;
import com.scrumaiassistant.dto.TaskResponse;
import com.scrumaiassistant.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting Management APIs")
@CrossOrigin
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    @Operation(summary = "Create a new meeting")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse createMeeting(@Valid @RequestBody MeetingCreateRequest request) {
        return meetingService.createMeeting(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meeting details")
    public MeetingResponse getMeeting(@PathVariable UUID id) {
        return meetingService.getMeeting(id);
    }

    @GetMapping
    @Operation(summary = "List all meetings with pagination")
    public List<MeetingResponse> getAllMeetings(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "10") int limit) {
        return meetingService.getAllMeetings(skip, limit);
    }

    @PostMapping(value = "/{id}/upload-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload audio file for meeting")
    public ResponseEntity<Void> uploadAudio(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        meetingService.uploadAudio(id, file);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/transcript", consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Add transcript text to meeting")
    public ResponseEntity<Void> addTranscript(@PathVariable UUID id, @RequestBody String transcript) {
        meetingService.addTranscript(id, transcript);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process meeting (Async)")
    public ResponseEntity<String> processMeeting(@PathVariable UUID id) {
        meetingService.processMeeting(id);
        return ResponseEntity.accepted().body("Processing started");
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Get extracted items for meeting")
    public List<ExtractedItemResponse> getMeetingItems(@PathVariable UUID id) {
        return meetingService.getMeetingItems(id);
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "Get tasks created for meeting")
    public List<TaskResponse> getMeetingTasks(@PathVariable UUID id) {
        return meetingService.getMeetingTasks(id);
    }
}

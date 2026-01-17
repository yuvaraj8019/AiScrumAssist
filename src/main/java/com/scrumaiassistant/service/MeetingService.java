package com.scrumaiassistant.service;

import com.scrumaiassistant.dto.ExtractedItemResponse;
import com.scrumaiassistant.dto.MeetingCreateRequest;
import com.scrumaiassistant.dto.MeetingResponse;
import com.scrumaiassistant.dto.TaskResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MeetingService {
    MeetingResponse createMeeting(MeetingCreateRequest request);
    MeetingResponse getMeeting(UUID id);
    List<MeetingResponse> getAllMeetings(int skip, int limit);
    void uploadAudio(UUID id, MultipartFile file);
    void addTranscript(UUID id, String transcript);
    void processMeeting(UUID id);
    List<ExtractedItemResponse> getMeetingItems(UUID id);
    List<TaskResponse> getMeetingTasks(UUID id);
}

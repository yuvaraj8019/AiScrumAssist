package com.scrumaiassistant.repository;

import com.scrumaiassistant.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    List<Meeting> findByMeetingDateAfter(LocalDateTime date);
}

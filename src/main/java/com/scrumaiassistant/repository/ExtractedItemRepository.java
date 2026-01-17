package com.scrumaiassistant.repository;

import com.scrumaiassistant.model.ExtractedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractedItemRepository extends JpaRepository<ExtractedItem, UUID> {
    List<ExtractedItem> findByMeetingId(UUID meetingId);
}

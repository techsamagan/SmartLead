package com.samagan.smartlead.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByStatusAndCreatedAtBefore(MessageStatus status, Instant createdBefore);

    @EntityGraph(attributePaths = "form")
    Page<Message> findByFormUserId(UUID userId, Pageable pageable);

    long countByFormUserIdAndCreatedAtAfter(UUID userId, Instant createdAfter);
}

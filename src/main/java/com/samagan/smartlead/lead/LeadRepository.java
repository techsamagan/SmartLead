package com.samagan.smartlead.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    // Both filters are optional; ownership is always enforced through the lead's form
    @EntityGraph(attributePaths = {"message", "message.form"})
    @Query("""
            select l from Lead l
            where l.message.form.user.id = :userId
              and (:type is null or l.type = :type)
              and (:urgency is null or l.urgency = :urgency)
            """)
    Page<Lead> findForUser(@Param("userId") UUID userId,
                           @Param("type") String type,
                           @Param("urgency") String urgency,
                           Pageable pageable);

    @EntityGraph(attributePaths = {"message", "message.form", "message.form.user"})
    @Query("select l from Lead l where l.id = :id")
    Optional<Lead> findWithOwnerById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"message", "message.form"})
    Optional<Lead> findByIdAndMessageFormUserId(UUID id, UUID userId);
}

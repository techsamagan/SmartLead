package com.samagan.smartlead.form;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormRepository extends JpaRepository<Form, UUID> {

    Optional<Form> findByFormKey(String formKey);

    List<Form> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Form> findByIdAndUserId(UUID id, UUID userId);
}

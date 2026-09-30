package com.cardiovet.patient;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findByNameContainingIgnoreCaseOrTutorNameContainingIgnoreCase(
            String name, String tutorName, Pageable pageable);

    Optional<Patient> findFirstByTutorIdAndNameIgnoreCase(UUID tutorId, String name);
}

package com.cardiovet.patient;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

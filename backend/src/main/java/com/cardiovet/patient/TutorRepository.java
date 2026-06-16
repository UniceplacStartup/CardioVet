package com.cardiovet.patient;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorRepository extends JpaRepository<Tutor, UUID> {
}

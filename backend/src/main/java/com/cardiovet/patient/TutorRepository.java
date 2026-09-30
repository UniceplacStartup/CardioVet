package com.cardiovet.patient;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorRepository extends JpaRepository<Tutor, UUID> {

    Optional<Tutor> findByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, UUID id);

    boolean existsByDocument(String document);

    Page<Tutor> findByNameContainingIgnoreCaseOrDocumentContaining(String name, String document, Pageable pageable);
}

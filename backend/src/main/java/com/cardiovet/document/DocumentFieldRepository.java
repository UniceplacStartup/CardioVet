package com.cardiovet.document;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFieldRepository extends JpaRepository<DocumentField, UUID> {
}

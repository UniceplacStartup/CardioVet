package com.cardiovet.patient;

import com.cardiovet.patient.dto.TutorRequest;
import com.cardiovet.patient.dto.TutorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutors")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Responsaveis pelos pacientes")
public class TutorController {

    private final TutorService tutorService;

    @GetMapping
    public Page<TutorResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return tutorService.list(search, pageable);
    }

    @GetMapping("/{id}")
    public TutorResponse get(@PathVariable UUID id) {
        return tutorService.get(id);
    }

    @PostMapping
    public ResponseEntity<TutorResponse> create(@Valid @RequestBody TutorRequest request) {
        TutorResponse created = tutorService.create(request);
        return ResponseEntity.created(URI.create("/api/tutors/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TutorResponse update(@PathVariable UUID id, @Valid @RequestBody TutorRequest request) {
        return tutorService.update(id, request);
    }
}

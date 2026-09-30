package com.cardiovet.patient;

import com.cardiovet.patient.dto.TutorRequest;
import com.cardiovet.patient.dto.TutorResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public Page<TutorResponse> list(String search, Pageable pageable) {
        Page<Tutor> page = (search == null || search.isBlank())
                ? tutorRepository.findAll(pageable)
                : tutorRepository.findByNameContainingIgnoreCaseOrDocumentContaining(search, normalizeDocument(search), pageable);
        return page.map(TutorResponse::from);
    }

    @Transactional(readOnly = true)
    public TutorResponse get(UUID id) {
        return TutorResponse.from(findOrThrow(id));
    }

    @Transactional
    public TutorResponse create(TutorRequest request) {
        String document = normalizeDocument(request.document());
        if (document != null && tutorRepository.existsByDocument(document)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe tutor com este documento");
        }
        Tutor tutor = Tutor.builder().build();
        apply(tutor, request, document);
        return TutorResponse.from(tutorRepository.save(tutor));
    }

    @Transactional
    public TutorResponse update(UUID id, TutorRequest request) {
        Tutor tutor = findOrThrow(id);
        String document = normalizeDocument(request.document());
        if (document != null && tutorRepository.existsByDocumentAndIdNot(document, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe tutor com este documento");
        }
        apply(tutor, request, document);
        return TutorResponse.from(tutor);
    }

    public static String normalizeDocument(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private void apply(Tutor tutor, TutorRequest request, String document) {
        tutor.setName(request.name().trim());
        tutor.setEmail(request.email());
        tutor.setPhone(request.phone());
        tutor.setDocument(document);
    }

    private Tutor findOrThrow(UUID id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor nao encontrado"));
    }
}

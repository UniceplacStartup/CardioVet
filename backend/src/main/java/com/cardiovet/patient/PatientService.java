package com.cardiovet.patient;

import com.cardiovet.patient.dto.PatientRequest;
import com.cardiovet.patient.dto.PatientResponse;
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
public class PatientService {

    private final PatientRepository patientRepository;
    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public Page<PatientResponse> list(String search, Pageable pageable) {
        Page<Patient> page = (search == null || search.isBlank())
                ? patientRepository.findAll(pageable)
                : patientRepository.findByNameContainingIgnoreCase(search, pageable);
        return page.map(PatientResponse::from);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(UUID id) {
        return PatientResponse.from(findOrThrow(id));
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        Tutor tutor = findTutorOrThrow(request.tutorId());
        Patient patient = Patient.builder()
                .tutor(tutor)
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .sex(request.sex())
                .birthDate(request.birthDate())
                .weightKg(request.weightKg())
                .build();
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(UUID id, PatientRequest request) {
        Patient patient = findOrThrow(id);
        if (!patient.getTutor().getId().equals(request.tutorId())) {
            patient.setTutor(findTutorOrThrow(request.tutorId()));
        }
        patient.setName(request.name());
        patient.setSpecies(request.species());
        patient.setBreed(request.breed());
        patient.setSex(request.sex());
        patient.setBirthDate(request.birthDate());
        patient.setWeightKg(request.weightKg());
        return PatientResponse.from(patient);
    }

    @Transactional
    public void delete(UUID id) {
        Patient patient = findOrThrow(id);
        patientRepository.delete(patient);
    }

    private Patient findOrThrow(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente nao encontrado"));
    }

    private Tutor findTutorOrThrow(UUID id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor nao encontrado"));
    }
}

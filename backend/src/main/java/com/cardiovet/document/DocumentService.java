package com.cardiovet.document;

import com.cardiovet.document.PdfExtractionService.ExtractedField;
import com.cardiovet.document.PdfExtractionService.ExtractionResult;
import com.cardiovet.document.dto.DocumentDetailResponse;
import com.cardiovet.document.dto.DocumentImageResponse;
import com.cardiovet.document.dto.DocumentResponse;
import com.cardiovet.document.dto.ReportRequest;
import com.cardiovet.patient.Patient;
import com.cardiovet.patient.PatientRepository;
import com.cardiovet.patient.Tutor;
import com.cardiovet.patient.TutorRepository;
import com.cardiovet.patient.TutorService;
import com.cardiovet.user.User;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final DocumentRepository documentRepository;
    private final DocumentImageRepository imageRepository;
    private final PatientRepository patientRepository;
    private final TutorRepository tutorRepository;
    private final PdfExtractionService extractionService;

    @Transactional
    public DocumentDetailResponse upload(MultipartFile file, UUID patientId, User uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo PDF obrigatorio");
        }
        if (!isPdf(file)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Apenas arquivos PDF sao aceitos");
        }

        byte[] bytes = readBytes(file);

        Patient patient = null;
        if (patientId != null) {
            patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente nao encontrado"));
        }

        Document document = Document.builder()
                .patient(patient)
                .uploadedBy(uploadedBy)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/pdf")
                .fileSizeBytes(bytes.length)
                .sha256(sha256(bytes))
                .content(bytes)
                .status(DocumentStatus.PENDENTE)
                .veterinarianName(uploadedBy.getName())
                .build();

        try {
            ExtractionResult result = extractionService.extract(bytes);
            document.setExtractedText(result.rawText());
            document.setDocumentDate(result.documentDate() != null ? result.documentDate() : LocalDate.now());
            document.setExamDate(result.documentDate());
            document.setFindings(result.findings());
            document.setConclusion(result.conclusion());
            for (ExtractedField f : result.fields()) {
                document.addField(DocumentField.builder()
                        .fieldKey(f.key())
                        .label(f.label())
                        .value(f.value())
                        .unit(f.unit())
                        .category(f.category())
                        .build());
            }
            document.setStatus(DocumentStatus.PROCESSADO);
        } catch (IOException | RuntimeException e) {
            document.setStatus(DocumentStatus.ERRO);
            document.setErrorMessage("Falha ao extrair PDF: " + e.getMessage());
            if (document.getDocumentDate() == null) {
                document.setDocumentDate(LocalDate.now());
            }
        }

        return DocumentDetailResponse.from(documentRepository.save(document), List.of());
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(LocalDate from, LocalDate to, UUID patientId, Pageable pageable) {
        LocalDate start = from != null ? from : LocalDate.of(1900, 1, 1);
        LocalDate end = to != null ? to : LocalDate.of(9999, 12, 31);
        return patientId != null
                ? documentRepository.searchByPatient(start, end, patientId, pageable)
                : documentRepository.search(start, end, pageable);
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse get(UUID id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Document getRaw(UUID id) {
        return findOrThrow(id);
    }

    @Transactional
    public DocumentDetailResponse updateReport(UUID id, ReportRequest request) {
        Document document = findOrThrow(id);

        Tutor tutor = resolveTutor(document, request);
        Patient patient = resolvePatient(document, tutor, request);

        document.setPatient(patient);
        document.setReportModel(blankToNull(request.reportModel()));
        document.setExamDate(request.examDate());
        if (request.issueDate() != null) {
            document.setDocumentDate(request.issueDate());
        }
        document.setPatientAge(blankToNull(request.patientAge()));
        document.setVeterinarianName(blankToNull(request.veterinarianName()));
        document.setFindings(blankToNull(request.findings()));
        document.setConclusion(blankToNull(request.conclusion()));

        return toDetail(document);
    }

    @Transactional
    public void delete(UUID id) {
        documentRepository.delete(findOrThrow(id));
    }

    @Transactional
    public List<DocumentImageResponse> addImages(UUID documentId, List<MultipartFile> files) {
        Document document = findOrThrow(documentId);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma imagem enviada");
        }
        List<DocumentImage> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            String type = file.getContentType() != null ? file.getContentType().toLowerCase() : null;
            if (type == null || !ALLOWED_IMAGE_TYPES.contains(type)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Apenas imagens PNG, JPEG, WebP ou GIF sao aceitas");
            }
            if (file.getSize() > MAX_IMAGE_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Imagem acima de 10 MB");
            }
            byte[] bytes = readBytes(file);
            saved.add(imageRepository.save(DocumentImage.builder()
                    .document(document)
                    .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "imagem")
                    .contentType(type)
                    .fileSizeBytes(bytes.length)
                    .content(bytes)
                    .build()));
        }
        return saved.stream()
                .map(i -> new DocumentImageResponse(i.getId(), i.getFileName(), i.getContentType(), i.getFileSizeBytes(), i.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentImage getImage(UUID documentId, UUID imageId) {
        return imageRepository.findByIdAndDocumentId(imageId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem nao encontrada"));
    }

    @Transactional
    public void deleteImage(UUID documentId, UUID imageId) {
        imageRepository.delete(getImage(documentId, imageId));
    }

    private Tutor resolveTutor(Document document, ReportRequest request) {
        String cpf = TutorService.normalizeDocument(request.tutorCpf());
        Tutor tutor = null;
        if (cpf != null) {
            tutor = tutorRepository.findByDocument(cpf).orElse(null);
        }
        if (tutor == null && document.getPatient() != null) {
            Tutor current = document.getPatient().getTutor();
            if (cpf == null || current.getDocument() == null) {
                tutor = current;
            }
        }
        if (tutor == null) {
            tutor = Tutor.builder().build();
        }
        tutor.setName(request.tutorName().trim());
        if (cpf != null) {
            tutor.setDocument(cpf);
        }
        return tutorRepository.save(tutor);
    }

    private Patient resolvePatient(Document document, Tutor tutor, ReportRequest request) {
        String name = request.patientName().trim();
        Patient current = document.getPatient();
        Patient patient;
        if (current != null && current.getTutor().getId().equals(tutor.getId())
                && current.getName().equalsIgnoreCase(name)) {
            patient = current;
        } else {
            patient = patientRepository.findFirstByTutorIdAndNameIgnoreCase(tutor.getId(), name)
                    .orElseGet(() -> Patient.builder().tutor(tutor).build());
        }
        patient.setName(name);
        patient.setSpecies(request.species().trim());
        patient.setBreed(blankToNull(request.breed()));
        patient.setSex(blankToNull(request.sex()));
        if (request.weightKg() != null) {
            patient.setWeightKg(request.weightKg());
        }
        return patientRepository.save(patient);
    }

    private DocumentDetailResponse toDetail(Document document) {
        return DocumentDetailResponse.from(document, imageRepository.findSummariesByDocumentId(document.getId()));
    }

    private Document findOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo", e);
        }
    }

    private boolean isPdf(MultipartFile file) {
        String type = file.getContentType();
        String name = file.getOriginalFilename();
        boolean typeOk = type != null && type.toLowerCase().contains("pdf");
        boolean nameOk = name != null && name.toLowerCase().endsWith(".pdf");
        return typeOk || nameOk;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

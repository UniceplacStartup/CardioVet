package com.cardiovet.document;

import com.cardiovet.document.PdfExtractionService.ExtractedField;
import com.cardiovet.document.PdfExtractionService.ExtractionResult;
import com.cardiovet.document.dto.DocumentDetailResponse;
import com.cardiovet.document.dto.DocumentResponse;
import com.cardiovet.patient.Patient;
import com.cardiovet.patient.PatientRepository;
import com.cardiovet.user.User;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
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

    private final DocumentRepository documentRepository;
    private final PatientRepository patientRepository;
    private final PdfExtractionService extractionService;

    /**
     * Recebe um PDF, salva o original e tenta extrair os campos do layout padrao.
     * Falhas de extracao nao impedem o salvamento — o documento fica com status ERRO.
     */
    @Transactional
    public DocumentDetailResponse upload(MultipartFile file, UUID patientId, User uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo PDF obrigatorio");
        }
        if (!isPdf(file)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Apenas arquivos PDF sao aceitos");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo", e);
        }

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
                .build();

        try {
            ExtractionResult result = extractionService.extract(bytes);
            document.setExtractedText(result.rawText());
            document.setDocumentDate(result.documentDate() != null ? result.documentDate() : LocalDate.now());
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

        return DocumentDetailResponse.from(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(LocalDate from, LocalDate to, UUID patientId, Pageable pageable) {
        Page<Document> page = (patientId != null)
                ? documentRepository.findByPatientId(patientId, pageable)
                : documentRepository.findByDateRange(from, to, pageable);
        return page.map(DocumentResponse::from);
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse get(UUID id) {
        return DocumentDetailResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Document getRaw(UUID id) {
        return findOrThrow(id);
    }

    @Transactional
    public void delete(UUID id) {
        documentRepository.delete(findOrThrow(id));
    }

    private Document findOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));
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
}

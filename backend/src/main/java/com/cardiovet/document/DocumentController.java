package com.cardiovet.document;

import com.cardiovet.document.dto.DocumentDetailResponse;
import com.cardiovet.document.dto.DocumentResponse;
import com.cardiovet.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Upload e extracao de informacoes de laudos PDF")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Envia um PDF e extrai os campos do layout padrao")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @AuthenticationPrincipal User user) {
        DocumentDetailResponse created = documentService.upload(file, patientId, user);
        return ResponseEntity.created(URI.create("/api/documents/" + created.id())).body(created);
    }

    @Operation(summary = "Lista documentos salvos, filtrando por intervalo de datas ou paciente")
    @GetMapping
    public Page<DocumentResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID patientId,
            @PageableDefault(size = 20, sort = "documentDate") Pageable pageable) {
        return documentService.list(from, to, patientId, pageable);
    }

    @Operation(summary = "Detalha um documento com os campos extraidos")
    @GetMapping("/{id}")
    public DocumentDetailResponse get(@PathVariable UUID id) {
        return documentService.get(id);
    }

    @Operation(summary = "Faz o download do PDF original")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Document document = documentService.getRaw(id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(document.getFileName() != null ? document.getFileName() : "documento.pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(document.getFileSizeBytes())
                .body(new ByteArrayResource(document.getContent()));
    }

    @Operation(summary = "Remove um documento")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

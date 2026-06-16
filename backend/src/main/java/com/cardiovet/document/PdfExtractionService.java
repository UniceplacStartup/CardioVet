package com.cardiovet.document;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

/**
 * Extrai texto e campos estruturados de um PDF de laudo de ecocardiografia
 * que segue um layout padrao (rotulo seguido de valor).
 *
 * <p>A extracao de campos e dirigida por configuracao ({@link #DEFINITIONS}):
 * cada definicao mapeia uma chave normalizada, rotulo legivel, categoria, unidade
 * e uma expressao regular cujo primeiro grupo de captura contem o valor.
 * Para suportar novos modelos de laudo basta acrescentar definicoes — sem mudar a logica.
 */
@Service
public class PdfExtractionService {

    /** Resultado da extracao: texto bruto + campos reconhecidos. */
    public record ExtractionResult(String rawText, LocalDate documentDate, List<ExtractedField> fields) {}

    public record ExtractedField(String key, String label, String value, String unit, String category) {}

    /** Categorias de campos do laudo. */
    private static final String PACIENTE = "PACIENTE";
    private static final String MODO_M = "MODO_M";
    private static final String DOPPLER = "DOPPLER";
    private static final String CALCULO = "CALCULO";

    /** Numero decimal aceitando virgula ou ponto (ex.: 38,5 ou 38.5). */
    private static final String NUM = "([0-9]+(?:[.,][0-9]+)?)";

    private record FieldDef(String key, String label, String category, String unit, Pattern pattern) {
        FieldDef(String key, String label, String category, String unit, String regex) {
            this(key, label, category, unit,
                    Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
    }

    /**
     * Definicoes do layout padrao. Os rotulos cobrem variacoes comuns
     * (abreviacoes em ingles e portugues) usadas em laudos de ecocardiografia veterinaria.
     */
    private static final List<FieldDef> DEFINITIONS = List.of(
            // ---- Identificacao do paciente ----
            new FieldDef("animalName", "Animal", PACIENTE, null,
                    "(?:Animal|Paciente|Nome\\s+do\\s+animal)\\s*[:\\-]\\s*(.+)"),
            new FieldDef("species", "Especie", PACIENTE, null,
                    "(?:Especie|Espécie)\\s*[:\\-]\\s*(.+)"),
            new FieldDef("breed", "Raca", PACIENTE, null,
                    "(?:Raca|Raça)\\s*[:\\-]\\s*(.+)"),
            new FieldDef("sex", "Sexo", PACIENTE, null,
                    "Sexo\\s*[:\\-]\\s*(.+)"),
            new FieldDef("tutor", "Tutor", PACIENTE, null,
                    "(?:Tutor|Propriet[aá]rio|Responsavel|Responsável)\\s*[:\\-]\\s*(.+)"),
            new FieldDef("weight", "Peso", PACIENTE, "kg",
                    "Peso\\s*[:\\-]?\\s*" + NUM + "\\s*kg"),
            new FieldDef("heartRate", "Frequencia cardiaca", PACIENTE, "bpm",
                    "(?:FC|Frequencia\\s+cardíaca|Freq\\.?\\s*card)\\s*[:\\-]?\\s*" + NUM + "\\s*(?:bpm)?"),

            // ---- Medidas Modo-M / 2D ----
            new FieldDef("AO", "Aorta", MODO_M, "mm",
                    "(?:AO|Aorta)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("LA", "Atrio esquerdo", MODO_M, "mm",
                    "(?:LA|AE|[AÁ]trio\\s+esquerdo)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("LA_AO", "Relacao LA/AO", CALCULO, null,
                    "(?:LA\\s*/\\s*AO|AE\\s*/\\s*AO)\\s*[:\\-]?\\s*" + NUM),
            new FieldDef("IVSd", "Septo interventricular (diastole)", MODO_M, "mm",
                    "(?:IVSd|SIVd)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("IVSs", "Septo interventricular (sistole)", MODO_M, "mm",
                    "(?:IVSs|SIVs)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("LVIDd", "Diametro interno VE (diastole)", MODO_M, "mm",
                    "(?:LVIDd|DIVEd|VEd)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("LVIDs", "Diametro interno VE (sistole)", MODO_M, "mm",
                    "(?:LVIDs|DIVEs|VEs)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("LVPWd", "Parede livre VE (diastole)", MODO_M, "mm",
                    "(?:LVPWd|PLVEd|PPd)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("LVPWs", "Parede livre VE (sistole)", MODO_M, "mm",
                    "(?:LVPWs|PLVEs|PPs)\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),

            // ---- Calculos de funcao sistolica ----
            new FieldDef("FS", "Fracao de encurtamento", CALCULO, "%",
                    "(?:FS|%FS|Fra[cç][aã]o\\s+de\\s+encurtamento)\\s*[:\\-]?\\s*" + NUM + "\\s*%?"),
            new FieldDef("EF", "Fracao de ejecao", CALCULO, "%",
                    "(?:EF|FE|%EF|Fra[cç][aã]o\\s+de\\s+eje[cç][aã]o)\\s*[:\\-]?\\s*" + NUM + "\\s*%?"),

            // ---- Doppler ----
            new FieldDef("mitralE", "Onda E mitral", DOPPLER, "m/s",
                    "(?:Onda\\s*E|Mitral\\s*E|E\\s*mitral)\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"),
            new FieldDef("mitralA", "Onda A mitral", DOPPLER, "m/s",
                    "(?:Onda\\s*A|Mitral\\s*A|A\\s*mitral)\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"),
            new FieldDef("E_A", "Relacao E/A", CALCULO, null,
                    "E\\s*/\\s*A\\s*[:\\-]?\\s*" + NUM),
            new FieldDef("aorticVel", "Velocidade aortica", DOPPLER, "m/s",
                    "(?:Velocidade\\s+a[oó]rtica|V\\.?\\s*a[oó]rtica|Ao\\s*Vmax)\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"),
            new FieldDef("pulmonaryVel", "Velocidade pulmonar", DOPPLER, "m/s",
                    "(?:Velocidade\\s+pulmonar|V\\.?\\s*pulmonar|Pulm\\s*Vmax)\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"));

    /** Datas no formato dd/MM/yyyy precedidas por um rotulo de data. */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?:Data(?:\\s+do\\s+exame)?|Exame|Realizado\\s+em)\\s*[:\\-]?\\s*"
                    + "(\\d{2}/\\d{2}/\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Le os bytes de um PDF, extrai o texto e reconhece os campos do layout padrao.
     *
     * @throws IOException se o arquivo nao for um PDF valido / legivel.
     */
    public ExtractionResult extract(byte[] pdfBytes) throws IOException {
        String rawText;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            rawText = stripper.getText(document);
        }
        return parse(rawText);
    }

    /** Reconhece campos e data a partir do texto ja extraido. Visivel para testes. */
    ExtractionResult parse(String rawText) {
        String normalized = rawText == null ? "" : rawText;

        // Mantem a primeira ocorrencia de cada chave (laudos repetem rotulos em legendas).
        Map<String, ExtractedField> found = new LinkedHashMap<>();
        for (FieldDef def : DEFINITIONS) {
            Matcher matcher = def.pattern().matcher(normalized);
            if (matcher.find()) {
                String value = matcher.group(1).trim().replaceAll("\\s+", " ");
                if (!value.isBlank()) {
                    found.putIfAbsent(def.key(),
                            new ExtractedField(def.key(), def.label(), value, def.unit(), def.category()));
                }
            }
        }

        LocalDate documentDate = parseDate(normalized);
        return new ExtractionResult(normalized, documentDate, new ArrayList<>(found.values()));
    }

    private LocalDate parseDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return LocalDate.parse(matcher.group(1), DATE_FMT);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}

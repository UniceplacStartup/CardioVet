package com.cardiovet.document;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    public record ExtractionResult(
            String rawText, LocalDate documentDate, List<ExtractedField> fields, String findings, String conclusion) {}

    public record ExtractedField(String key, String label, String value, String unit, String category) {}

    /** Categorias de campos do laudo. */
    private static final String PACIENTE = "PACIENTE";
    private static final String MODO_M = "MODO_M";
    private static final String DOPPLER = "DOPPLER";
    private static final String CALCULO = "CALCULO";

    /** Numero decimal aceitando virgula ou ponto (ex.: 38,5 ou 38.5). */
    private static final String NUM = "([0-9]+(?:[.,][0-9]+)?)";

    private static final String TXT = "([^\\n:]+?)(?=\\s+\\p{L}+\\s*:|\\s*$)";

    private record FieldDef(String key, String label, String category, String unit, Pattern pattern, int group) {
        FieldDef(String key, String label, String category, String unit, String regex) {
            this(key, label, category, unit, regex, 1);
        }

        FieldDef(String key, String label, String category, String unit, String regex, int group) {
            this(key, label, category, unit,
                    Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE),
                    group);
        }
    }

    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(ESTUDO\\s+DOPPLER|OBSERVA[CÇ][OÕ]ES|CONCLUS[AÃ]O)\\s*:(.*?)"
                    + "(?=\\n\\s*[A-ZÀ-Ú][A-ZÀ-Ú ]{3,}:|\\n\\s*\\d+\\s*\\n|\\n\\s*Nome\\s*:|\\z)",
            Pattern.DOTALL);

    /**
     * Definicoes do layout padrao. Os rotulos cobrem variacoes comuns
     * (abreviacoes em ingles e portugues) usadas em laudos de ecocardiografia veterinaria.
     */
    private static final List<FieldDef> DEFINITIONS = List.of(
            // ---- Identificacao do paciente ----
            new FieldDef("animalName", "Animal", PACIENTE, null,
                    "\\b(?:Animal|Paciente|Nome(?:\\s+do\\s+animal)?)\\s*[:\\-]\\s*" + TXT),
            new FieldDef("species", "Especie", PACIENTE, null,
                    "\\b(?:Especie|Espécie)\\s*[:\\-]\\s*" + TXT),
            new FieldDef("breed", "Raca", PACIENTE, null,
                    "\\b(?:Raca|Raça)\\s*[:\\-]\\s*" + TXT),
            new FieldDef("sex", "Sexo", PACIENTE, null,
                    "\\bSexo\\s*[:\\-]\\s*" + TXT),
            new FieldDef("age", "Idade", PACIENTE, null,
                    "\\bIdade\\s*[:\\-]\\s*" + TXT),
            new FieldDef("tutor", "Tutor", PACIENTE, null,
                    "\\b(?:Tutor|Propriet[aá]rio|Responsavel|Responsável)\\s*[:\\-]\\s*" + TXT),
            new FieldDef("requester", "Solicitante", PACIENTE, null,
                    "\\bSolicitante\\s*[:\\-]\\s*" + TXT),
            new FieldDef("weight", "Peso", PACIENTE, "kg",
                    "Peso\\s*[:\\-]?\\s*" + NUM + "\\s*kg"),
            new FieldDef("heartRate", "Frequencia cardiaca", PACIENTE, "bpm",
                    "(?:\\bFC|Frequ[eê]ncia\\s+card[ií]aca|Freq\\.?\\s*card)\\s*[:\\-]?\\s*" + NUM + "\\s*(?:bpm)?"),
            new FieldDef("rhythm", "Ritmo", PACIENTE, null,
                    "\\bRitmo\\s*[:\\-]\\s*" + TXT),

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
                    "(?:Velocidade\\s+pulmonar|V\\.?\\s*pulmonar|Pulm\\s*Vmax)\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"),

            new FieldDef("AO", "Aorta", MODO_M, "cm",
                    "\\bAorta\\s+AE\\s*" + NUM + "\\s*cm\\s+" + NUM + "\\s*cm", 1),
            new FieldDef("LA", "Atrio esquerdo", MODO_M, "cm",
                    "\\bAorta\\s+AE\\s*" + NUM + "\\s*cm\\s+" + NUM + "\\s*cm", 2),
            new FieldDef("LA_AO", "Relacao LA/AO", CALCULO, null,
                    "Rela[cç][aã]o\\s+[AÁ]trio\\s+Esquerdo\\s*/\\s*Aorta\\s*[:\\-]?\\s*" + NUM),
            new FieldDef("LAmax", "Diametro maximo do AE", MODO_M, "cm",
                    "Di[aâ]metro\\s+M[aá]ximo\\s+do\\s+AE\\s*[:\\-]?\\s*" + NUM + "\\s*cm"),
            new FieldDef("LA_FS", "Fracao de encurtamento atrial", CALCULO, "%",
                    "Fun[cç][aã]o\\s*\\(FENC\\)\\s*Atrial\\s*[:\\-]?\\s*" + NUM + "\\s*%"),
            new FieldDef("IVSd", "Septo interventricular (diastole)", MODO_M, "cm",
                    "\\bSIVd\\s+(?:\\p{L}+\\s+)?" + NUM + "\\s*cm"),
            new FieldDef("IVSs", "Septo interventricular (sistole)", MODO_M, "cm",
                    "\\bSIVs\\s+(?:\\p{L}+\\s+)?" + NUM + "\\s*cm"),
            new FieldDef("LVPWd", "Parede livre VE (diastole)", MODO_M, "cm",
                    "\\bPLd\\s+(?:\\p{L}+\\s+)?" + NUM + "\\s*cm"),
            new FieldDef("LVPWs", "Parede livre VE (sistole)", MODO_M, "cm",
                    "\\bPLs\\s+(?:\\p{L}+\\s+)?" + NUM + "\\s*cm"),
            new FieldDef("LVIDd", "Diametro interno VE (diastole)", MODO_M, "cm",
                    "\\bDi[aâ]metro\\s+Diast[oó]lico\\s*[:\\-]?\\s*" + NUM + "\\s*cm"),
            new FieldDef("LVIDs", "Diametro interno VE (sistole)", MODO_M, "cm",
                    "\\bDi[aâ]metro\\s+Sist[oó]lico\\s*[:\\-]?\\s*" + NUM + "\\s*cm"),
            new FieldDef("LVIDdN", "Diametro diastolico VE normalizado", CALCULO, null,
                    "Diam\\.?\\s+Diast[oó]lico\\s+do\\s+VE\\s+normalizado\\s+pelo\\s+peso\\s*[:\\-]?\\s*" + NUM),
            new FieldDef("EPSS", "Distancia E-septo", MODO_M, "mm",
                    "Dist[aâ]ncia\\s+E\\s*-\\s*Septo\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("TAPSE", "TAPSE", MODO_M, "mm",
                    "\\bTAPSE\\s*[:\\-]?\\s*" + NUM + "\\s*mm"),
            new FieldDef("aorticVel", "Velocidade aortica", DOPPLER, "m/s",
                    "Fluxo\\s+A[oó]rtico\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"),
            new FieldDef("aorticGradient", "Gradiente de pressao aortico", DOPPLER, "mmHg",
                    "Fluxo\\s+A[oó]rtico\\s*[:\\-]?\\s*" + NUM + "\\s*m/s\\s+" + NUM + "\\s*mmHg", 2),
            new FieldDef("pulmonaryVel", "Velocidade pulmonar", DOPPLER, "m/s",
                    "Fluxo\\s+Pulmonar\\s*[:\\-]?\\s*" + NUM + "\\s*m/s"),
            new FieldDef("pulmonaryGradient", "Gradiente de pressao pulmonar", DOPPLER, "mmHg",
                    "Fluxo\\s+Pulmonar\\s*[:\\-]?\\s*" + NUM + "\\s*m/s\\s+" + NUM + "\\s*mmHg", 2),
            new FieldDef("IVRT", "Tempo de relaxamento isovolumetrico (TRIV)", DOPPLER, "ms",
                    "\\bTRIV\\s*[:\\-]?\\s*" + NUM + "\\s*ms"),
            new FieldDef("mitralEprime", "Onda E' (tecidual mitral)", DOPPLER, "cm/s",
                    "Onda\\s*E[’'′]\\s*[:\\-]?\\s*" + NUM + "\\s*cm/s"),
            new FieldDef("mitralAprime", "Onda A' (tecidual mitral)", DOPPLER, "cm/s",
                    "Onda\\s*A[’'′]\\s*[:\\-]?\\s*" + NUM + "\\s*cm/s"));

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
                String value = matcher.group(def.group()).trim().replaceAll("\\s+", " ");
                if (!value.isBlank()) {
                    found.putIfAbsent(def.key(),
                            new ExtractedField(def.key(), def.label(), value, def.unit(), def.category()));
                }
            }
        }

        LocalDate documentDate = parseDate(normalized);
        Map<String, String> sections = parseSections(normalized);
        String doppler = sections.get("ESTUDO DOPPLER");
        String observations = sections.get("OBSERVACOES");
        String findings = doppler == null ? observations
                : observations == null ? "Estudo Doppler:\n" + doppler
                : "Estudo Doppler:\n" + doppler + "\n\nObservações:\n" + observations;
        return new ExtractionResult(
                normalized, documentDate, new ArrayList<>(found.values()), findings, sections.get("CONCLUSAO"));
    }

    private Map<String, String> parseSections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher matcher = SECTION_PATTERN.matcher(text.replace("\r", ""));
        while (matcher.find()) {
            String key = Normalizer.normalize(matcher.group(1), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replaceAll("\\s+", " ")
                    .toUpperCase();
            String body = matcher.group(2).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.joining("\n"));
            if (!body.isEmpty()) {
                sections.putIfAbsent(key, body);
            }
        }
        return sections;
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

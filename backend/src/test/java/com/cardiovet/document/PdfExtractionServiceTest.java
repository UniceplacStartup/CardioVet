package com.cardiovet.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.cardiovet.document.PdfExtractionService.ExtractedField;
import com.cardiovet.document.PdfExtractionService.ExtractionResult;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PdfExtractionServiceTest {

    private static final String LAUDO = """
            Nome: Mimi Espécie: Fel Idade: 9a
            Raça: Siamês Peso: 4,1 Kg
            Responsável: Ana Lima
            Solicitante: Clinica Teste Data: 10/03/2026

            LAUDO ECOCARDIOGRÁFICO
            Frequência Cardíaca: 180 bpm Ritmo: Regular
            2 – Análise Atrial Esquerda Aorta AE
             0,80cm 0,95cm
            Relação Átrio Esquerdo/Aorta 1,19
            Diâmetro Máximo do AE 1,10cm (Valor Ref.:1,57cm)
            Função (FENC) Atrial 40% ( Valor Ref.: ≥ 25%).
            SIVd Normal 0,45 cm
            SIVs Normal 0,70 cm
            PLd Normal 0,40 cm
            PLs Normal 0,60 cm
            Diâmetro Diastólico  1,50 cm
            Diâmetro Sistólico  0,80 cm
            Diam. Diastólico do VE normalizado pelo peso 0,95
            Fração de Encurtamento  47%
            Fração de Ejeção  80%
            Distância E-Septo 0,50mm
            TAPSE 8,10 mm
            Fluxo Aórtico 0,90 m/s 3,24 mmHg
            Fluxo Pulmonar 0,75 m/s 2,25 mmHg
            Velocidade Onda E 0,60 m/s
            Velocidade Onda A 0,50 m/s
            Relação E/A 1,20
            TRIV 50ms
            Onda E’ 9,10cm/s
            Onda A’ - cm/s

            1

            Nome: Mimi Espécie: Fel Idade: 9a
            ESTUDO DOPPLER:
            ● Fluxos laminares;
            OBSERVAÇÕES:
            - Função sistólica preservada;
            - Espessura dentro do esperado (valor de
            referência: < 0,6 cm);
            CONCLUSÃO:
            - Ecocardiograma normal.

            2
            """;

    private final PdfExtractionService service = new PdfExtractionService();

    @Test
    void extraiIdentificacaoComVariosRotulosNaMesmaLinha() {
        Map<String, ExtractedField> f = fields(service.parse(LAUDO));

        assertThat(f.get("animalName").value()).isEqualTo("Mimi");
        assertThat(f.get("species").value()).isEqualTo("Fel");
        assertThat(f.get("age").value()).isEqualTo("9a");
        assertThat(f.get("breed").value()).isEqualTo("Siamês");
        assertThat(f.get("weight").value()).isEqualTo("4,1");
        assertThat(f.get("tutor").value()).isEqualTo("Ana Lima");
        assertThat(f.get("requester").value()).isEqualTo("Clinica Teste");
        assertThat(f.get("heartRate").value()).isEqualTo("180");
        assertThat(f.get("rhythm").value()).isEqualTo("Regular");
    }

    @Test
    void extraiMedidasEmCentimetrosEDoppler() {
        Map<String, ExtractedField> f = fields(service.parse(LAUDO));

        assertThat(f.get("AO").value()).isEqualTo("0,80");
        assertThat(f.get("AO").unit()).isEqualTo("cm");
        assertThat(f.get("LA").value()).isEqualTo("0,95");
        assertThat(f.get("LA_AO").value()).isEqualTo("1,19");
        assertThat(f.get("IVSd").value()).isEqualTo("0,45");
        assertThat(f.get("LVPWs").value()).isEqualTo("0,60");
        assertThat(f.get("LVIDd").value()).isEqualTo("1,50");
        assertThat(f.get("LVIDs").value()).isEqualTo("0,80");
        assertThat(f.get("FS").value()).isEqualTo("47");
        assertThat(f.get("EF").value()).isEqualTo("80");
        assertThat(f.get("TAPSE").value()).isEqualTo("8,10");
        assertThat(f.get("aorticVel").value()).isEqualTo("0,90");
        assertThat(f.get("aorticGradient").value()).isEqualTo("3,24");
        assertThat(f.get("pulmonaryGradient").value()).isEqualTo("2,25");
        assertThat(f.get("mitralE").value()).isEqualTo("0,60");
        assertThat(f.get("E_A").value()).isEqualTo("1,20");
        assertThat(f.get("IVRT").value()).isEqualTo("50");
        assertThat(f.get("mitralEprime").value()).isEqualTo("9,10");
        assertThat(f).doesNotContainKey("mitralAprime");
    }

    @Test
    void extraiDataEBlocosDeTexto() {
        ExtractionResult result = service.parse(LAUDO);

        assertThat(result.documentDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(result.findings()).contains("● Fluxos laminares;", "- Função sistólica preservada;", "referência: < 0,6 cm);");
        assertThat(result.findings()).doesNotContain("CONCLUSÃO", "Nome:");
        assertThat(result.conclusion()).isEqualTo("- Ecocardiograma normal.");
    }

    @Test
    void mantemCompatibilidadeComLayoutEmMilimetros() {
        Map<String, ExtractedField> f = fields(service.parse("""
                Paciente: Thor
                AO: 22 mm
                LVIDd: 45 mm
                """));

        assertThat(f.get("animalName").value()).isEqualTo("Thor");
        assertThat(f.get("AO").value()).isEqualTo("22");
        assertThat(f.get("AO").unit()).isEqualTo("mm");
        assertThat(f.get("LVIDd").unit()).isEqualTo("mm");
    }

    private static Map<String, ExtractedField> fields(ExtractionResult result) {
        return result.fields().stream().collect(Collectors.toMap(ExtractedField::key, f -> f));
    }
}

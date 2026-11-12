package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bilan économique en euros")
public record FinancesDTO(
    double coutCarburant,
    double coutChauffeur,
    double coutTotal,
    double chiffreAffaires,
    double marge
) {
}

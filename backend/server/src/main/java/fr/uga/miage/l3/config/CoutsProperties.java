package fr.uga.miage.l3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Hypothèses économiques par défaut, surchargeables dans {@code application.yml} (préfixe {@code app.couts}).
 *
 * @param consommationLitresAux100Km consommation utilisée quand aucun véhicule n'est affecté
 * @param prixCarburantParLitre      prix du gazole en euros
 * @param tauxHoraireChauffeur       coût horaire chargé d'un chauffeur sans taux personnalisé
 * @param tarifParLivraison          prix facturé au client pour une livraison
 * @param tarifParKg                 supplément facturé par kilogramme transporté
 */
@ConfigurationProperties(prefix = "app.couts")
public record CoutsProperties(
    @DefaultValue("8.0") double consommationLitresAux100Km,
    @DefaultValue("1.80") double prixCarburantParLitre,
    @DefaultValue("22.0") double tauxHoraireChauffeur,
    @DefaultValue("9.0") double tarifParLivraison,
    @DefaultValue("0.15") double tarifParKg
) {
}

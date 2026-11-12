package fr.uga.miage.l3.services;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Produit la feuille de route d'un chauffeur : une ligne par arrêt, dans l'ordre de passage.
 *
 * <p>Séparateur « ; » et BOM UTF-8 : Excel en français ouvre ainsi le fichier sans réglage.
 */
@Service
public class FeuilleDeRouteService {

    private static final char UTF8_BOM = (char) 0xFEFF;
    private static final String[] HEADERS = {
        "Ordre", "Référence", "Adresse", "Code postal", "Ville", "Poids (kg)", "Montage (min)", "Livrée"
    };

    public byte[] toCsv(TourneeEntity tournee) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader(HEADERS).get();
        List<LivraisonEntity> livraisons = tournee.getLivraisonEntities() == null ? List.of() : tournee.getLivraisonEntities();
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            // Le BOM doit précéder l'en-tête, que CSVPrinter écrit dès sa construction.
            writer.write(UTF8_BOM);
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (int i = 0; i < livraisons.size(); i++) {
                    printer.printRecord(row(i + 1, livraisons.get(i)));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Impossible de générer la feuille de route", ex);
        }
        return output.toByteArray();
    }

    public String fileName(TourneeEntity tournee) {
        String date = tournee.getSolutionEntity() == null || tournee.getSolutionEntity().getDate() == null
            ? "sans-date"
            : tournee.getSolutionEntity().getDate().toString();
        return "feuille-de-route-" + date + "-tournee-" + tournee.getId() + ".csv";
    }

    private static List<Object> row(int ordre, LivraisonEntity livraison) {
        AdresseEntity adresse = livraison.getAdresseEntity();
        String rue = (blankIfNull(adresse.getNumero()) + " " + blankIfNull(adresse.getRue())).trim();
        return List.of(
            ordre,
            blankIfNull(livraison.getReference()),
            rue,
            blankIfNull(adresse.getCodePostal()),
            blankIfNull(adresse.getVille()),
            String.format(Locale.FRANCE, "%.1f", livraison.getPoids() == null ? 0.0 : livraison.getPoids()),
            livraison.getTempsMontage() == null ? 0 : livraison.getTempsMontage().toMinutes(),
            livraison.isLivree() ? "oui" : "non"
        );
    }

    private static String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}

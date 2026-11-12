package fr.uga.miage.l3.services;

import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.models.entities.PhotoEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Pattern utilisé : Factory. Transforme un fichier envoyé en photo enregistrable.
 *
 * <p>Le format est déduit des premiers octets du fichier et non de l'en-tête envoyé par le
 * navigateur, que n'importe qui peut falsifier.
 */
@Component
public class PhotoFactory {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    public PhotoEntity from(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Aucune photo reçue");
        }
        if (file.getSize() > PhotoEntity.MAX_BYTES) {
            throw new InvalidRequestException("La photo dépasse 3 Mo");
        }
        byte[] data = read(file);
        String contentType = detectType(data)
            .orElseThrow(() -> new InvalidRequestException("Format non pris en charge : JPEG, PNG ou WebP uniquement"));
        return PhotoEntity.builder().contentType(contentType).data(data).build();
    }

    static Optional<String> detectType(byte[] data) {
        if (startsWith(data, 0, JPEG)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(data, 0, PNG)) {
            return Optional.of("image/png");
        }
        if (startsWith(data, 0, RIFF) && startsWith(data, 8, WEBP)) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] signature) {
        return data.length >= offset + signature.length
            && Arrays.equals(data, offset, offset + signature.length, signature, 0, signature.length);
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Lecture de la photo impossible", ex);
        }
    }
}

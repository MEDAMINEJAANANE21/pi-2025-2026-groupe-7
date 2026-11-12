package fr.uga.miage.l3.services;

import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.models.entities.PhotoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoFactoryTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    private final PhotoFactory factory = new PhotoFactory();

    @Test
    void reconnaitLeFormatAuxPremiersOctets() {
        assertThat(PhotoFactory.detectType(PNG)).contains("image/png");
        assertThat(PhotoFactory.detectType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).contains("image/jpeg");
        assertThat(PhotoFactory.detectType("RIFF1234WEBPVP8 ".getBytes())).contains("image/webp");
        assertThat(PhotoFactory.detectType("GIF89a".getBytes())).isEmpty();
        assertThat(PhotoFactory.detectType(new byte[0])).isEmpty();
    }

    @Test
    void ignoreLeTypeAnnonceParLeNavigateur() {
        // Le fichier se dit JPEG mais contient un PNG : c'est le contenu qui fait foi.
        PhotoEntity photo = factory.from(new MockMultipartFile("photo", "photo.jpg", "image/jpeg", PNG));

        assertThat(photo.getContentType()).isEqualTo("image/png");
        assertThat(photo.getData()).isEqualTo(PNG);
    }

    @Test
    void refuseUnFichierQuiNEstPasUneImage() {
        MockMultipartFile texte = new MockMultipartFile("photo", "photo.png", "image/png", "bonjour".getBytes());

        assertThatThrownBy(() -> factory.from(texte))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("JPEG, PNG ou WebP");
    }

    @Test
    void refuseUnFichierVideOuTropLourd() {
        assertThatThrownBy(() -> factory.from(new MockMultipartFile("photo", new byte[0])))
            .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> factory.from(new MockMultipartFile("photo", new byte[PhotoEntity.MAX_BYTES + 1])))
            .hasMessageContaining("3 Mo");
    }
}

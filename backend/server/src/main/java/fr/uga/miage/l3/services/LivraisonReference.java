package fr.uga.miage.l3.services;

import java.util.Locale;
import java.util.UUID;

/**
 * Référence courte et lisible au téléphone, comme « LIV-3F9A12C0 ».
 * Huit caractères hexadécimaux laissent quatre milliards de valeurs : largement assez ici.
 */
public final class LivraisonReference {

    private static final String PREFIX = "LIV-";
    private static final int LENGTH = 8;

    private LivraisonReference() {
    }

    public static String next() {
        return PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, LENGTH).toUpperCase(Locale.ROOT);
    }
}

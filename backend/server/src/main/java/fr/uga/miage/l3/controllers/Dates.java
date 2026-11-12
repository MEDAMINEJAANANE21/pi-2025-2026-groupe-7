package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.exceptions.InvalidRequestException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Lecture des dates reçues dans les URL, avec un message d'erreur lisible.
 */
final class Dates {

    private Dates() {
    }

    static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Date manquante (format attendu : yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException("Date invalide : '" + value + "' (format attendu : yyyy-MM-dd)");
        }
    }

    static LocalDate parseOptional(String value) {
        return value == null || value.isBlank() ? null : parse(value);
    }
}

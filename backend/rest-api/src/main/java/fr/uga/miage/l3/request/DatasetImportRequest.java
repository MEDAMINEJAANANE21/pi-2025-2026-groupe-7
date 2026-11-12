package fr.uga.miage.l3.request;

public record DatasetImportRequest(
    String dataset,
    Boolean clearBeforeImport
) {
}
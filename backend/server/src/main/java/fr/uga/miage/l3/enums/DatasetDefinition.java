package fr.uga.miage.l3.enums;

import java.util.Arrays;

public enum DatasetDefinition {
  DATASET_50_PAR_3_EXEMPLE_1(
    "dataset-50-par-3-exemple-1.csv",
    "distance-matrix-dataset-50-par-3-exemple-1.csv",
    "duration-matrix-dataset-50-par-3-exemple-1.csv"
  ),
  DATASET_50_PAR_3_EXEMPLE_2(
    "dataset-50-par-3-exemple-2.csv",
    "distance-matrix-dataset-50-par-3-exemple-2.csv",
    "duration-matrix-dataset-50-par-3-exemple-2.csv"
  ),
  DATASET_50_PAR_3_EXEMPLE_3(
    "dataset-50-par-3-exemple-3.csv",
    "distance-matrix-dataset-50-par-3-exemple-3.csv",
    "duration-matrix-dataset-50-par-3-exemple-3.csv"
  ),
  DATASET_100_PAR_3_EXEMPLE_1(
    "dataset-100-par-3-exemple-1.csv",
    "distance-matrix-dataset-100-par-3-exemple-1.csv",
    "duration-matrix-dataset-100-par-3-exemple-1.csv"
  ),
  DATASET_100_PAR_3_EXEMPLE_2(
    "dataset-100-par-3-exemple-2.csv",
    "distance-matrix-dataset-100-par-3-exemple-2.csv",
    "duration-matrix-dataset-100-par-3-exemple-2.csv"
  ),
  DATASET_400_PAR_10_EXEMPLE_1(
    "dataset-400-par-10-exemple-1.csv",
    "distance-matrix-dataset-400-par-3-exemple-1.csv",
    "duration-matrix-dataset-400-par-3-exemple-1.csv"
  ),
  DATASET_400_PAR_10_EXEMPLE_2(
    "dataset-400-par-10-exemple-2.csv",
    "distance-matrix-dataset-400-par-3-exemple-2.csv",
    "duration-matrix-dataset-400-par-3-exemple-2.csv"
  );

  private final String addressesFile;
  private final String distanceMatrixFile;
  private final String durationMatrixFile;

  DatasetDefinition(String addressesFile, String distanceMatrixFile, String durationMatrixFile) {
    this.addressesFile = addressesFile;
    this.distanceMatrixFile = distanceMatrixFile;
    this.durationMatrixFile = durationMatrixFile;
  }

  public String getAddressesFile() {
    return addressesFile;
  }

  public String getDistanceMatrixFile() {
    return distanceMatrixFile;
  }

  public String getDurationMatrixFile() {
    return durationMatrixFile;
  }

  public static DatasetDefinition fromValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Dataset value cannot be null or blank");
    }

    String normalized = value.trim().toUpperCase().replace('-', '_');

    return Arrays.stream(values())
      .filter(dataset -> dataset.name().equals(normalized))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown dataset: " + value));
  }
}
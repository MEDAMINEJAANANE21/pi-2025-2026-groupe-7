package fr.uga.miage.l3.models.domain;

import java.time.Duration;

public record Matrices(
  double[][] distances,
  Duration[][] durees
) {
}

package fr.uga.miage.l3.models.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coords {
  private double latitude;
  private double longitude;
}


package fr.uga.miage.l3.models.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statistic {
  private Long carburant;
  private Long distance;
  private Duration tempsTrajet;
  private Duration tempsMontage;
}

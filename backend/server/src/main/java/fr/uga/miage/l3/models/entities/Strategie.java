package fr.uga.miage.l3.models.entities;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.enums.Separation;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@AllArgsConstructor
@NoArgsConstructor
public class Strategie {

  @Enumerated(EnumType.STRING)
  private Separation separation;

  @Enumerated(EnumType.STRING)
  private Optimisation optimisation;

  private Integer nbVehicules;

  private Duration maxTempsParVehicule;
}

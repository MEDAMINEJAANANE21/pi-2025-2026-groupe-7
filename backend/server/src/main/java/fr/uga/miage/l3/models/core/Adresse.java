package fr.uga.miage.l3.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Adresse {
    public Coords coords;
    public String name;
    public String postCode;
    public String city;
}

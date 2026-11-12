package fr.uga.miage.l3.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResult {
    public int code;
    public List<VehicleRoute> routes;
    public List<Object> unassigned; // Keep as Object for unknown structure
    public Summary summary;
}

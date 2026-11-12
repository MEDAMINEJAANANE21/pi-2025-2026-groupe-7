package fr.uga.miage.l3.services.core.separation.kmedoids;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class KMedoidsResult {
    private Map<Integer, List<Integer>> clusters;
    private List<Integer> medoids;
    private int[] assignments;
    private double totalCost;
    private int iterations;
    private boolean converged;
}


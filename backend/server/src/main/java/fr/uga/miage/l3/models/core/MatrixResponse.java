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
public class MatrixResponse {
    public List<List<Integer>> distances;
    public List<List<Integer>> durations;
    public List<LocationRef> destinations;
    public List<LocationRef> sources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationRef {
        public double[] location; // [lng, lat]
        public String name;
        public int snapped_distance;
    }
}

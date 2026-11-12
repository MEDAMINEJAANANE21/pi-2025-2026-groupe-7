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
public class DirectionsResponse {
    private String type; // "FeatureCollection"
    //private DirectionsBbox bbox;
    private List<DirectionsFeature> features;
    ///private DirectionsMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsFeature {
        //private DirectionsBbox bbox;
        private String type; // "Feature"
        //private DirectionsProperties properties;
        private DirectionsGeometry geometry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsProperties {
        private List<DirectionsSegment> segments;
        private  DirectionExtra extras;
        private  List<DirectionWarning> warnings;
        private int[] way_points;
        private DirectionsSummary summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsGeometry {
        private List<double[]> coordinates; // [lng, lat, elevation?]
        private String type; // "LineString"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsSegment {
        private double distance;
        private double duration;
        private List<DirectionsStep> steps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionExtra {
        private List<int[]> values;
        private List<DirectionExtraSummary> summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionExtraSummary {
        private double value;
        private double distance;
        private double amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionRoadAccessRestriction {
        private DirectionRoadAccessRestriction roadaccessrestriction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionWarning {
        private int code;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsStep {
        private double distance;
        private double duration;
        private int type;
        private String instruction;
        private String name;
        private int[] way_points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsSummary {
        private double distance;
        private double duration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsBbox {
        private double[] bbox; // [minLng, minLat, maxLng, maxLat]
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsMetadata {
        private String attribution;
        private String service;
        private long timestamp;
        private DirectionsQuery query;
        private DirectionsEngine engine;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsQuery {
        private List<double[]> coordinates; // [lng, lat]
        private String profile;
        private String profileName;
        private String format;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionsEngine {
        private String version;
        private String build_date;
        private String graph_date;
        private String osm_date;
    }
}

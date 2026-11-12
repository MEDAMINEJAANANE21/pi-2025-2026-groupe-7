package fr.uga.miage.l3.services.core.separation;

import fr.uga.miage.l3.models.core.Adresse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AngularSeparationStrategy implements SeparationStrategy {
    @Override
    public List<List<Integer>> separate(List<Adresse> allLocations, double[][] distanceMatrix, int nbGroups) {
        if (allLocations == null || allLocations.isEmpty() || nbGroups <= 0) {
            return new ArrayList<>();
        }

        Adresse parking = allLocations.get(allLocations.size() - 1);
        int jobsLimitPerRequest = (int) Math.ceil((double) (allLocations.size() - 1) / nbGroups);

        List<AngleEntry> entries = new ArrayList<>();
        for (int i = 0; i < allLocations.size() - 1; i++) {
            Adresse a = allLocations.get(i);
            double dx = a.coords.lng - parking.coords.lng;
            double dy = a.coords.lat - parking.coords.lat;
            double theta = Math.toDegrees(Math.atan2(dy, dx));
            theta = (theta + 360) % 360;
            entries.add(new AngleEntry(i, theta));
        }

        entries.sort(Comparator.comparingDouble(e -> e.angle));
        List<List<Integer>> parts = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += jobsLimitPerRequest) {
            List<Integer> part = new ArrayList<>();
            for (int j = i; j < Math.min(i + jobsLimitPerRequest, entries.size()); j++) {
                part.add(entries.get(j).index);
            }
            parts.add(part);
        }
        return parts;
    }

    private record AngleEntry(int index, double angle) { }
}

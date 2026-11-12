package fr.uga.miage.l3.models.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStep {
    public String type; // "start", "end", or "job"
    public int arrival;
    public Integer id; // Optional for jobs
    public double[] location; // [lng, lat]
    public int service;
    public int setup;
}

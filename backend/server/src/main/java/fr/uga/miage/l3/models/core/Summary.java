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
public class Summary {
    public ComputingTimes computing_times;
    public int cost;
    public int duration;
    public int priority;
    public int service;
    public int setup;
    public List<Object> violations; // Keep as Object for unknown structure
    public int waiting_time;
}

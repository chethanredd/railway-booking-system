package com.railway.booking.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TrainStatusDTO {

    private String trainNumber;
    private String trainName;
    private String journeyDate; // e.g. 2026-04-06

    // LIVE | CACHED | SCHEDULED
    private String dataSource;

    // Overall status string, e.g. "Arrived at Bhopal, 10 mins late" or "Not Started"
    private String currentStatusMessage;
    
    // The exact station it just passed or is currently at
    private String currentStationCode;
    private String currentStationName;
    
    // Geographical interpolation details
    private double totalDistanceKm;
    private double coveredDistanceKm;

    private int currentDelayMinutes;
    
    private boolean isTerminated; // Reached final destination

    private List<StationNode> timeline;

    @Data
    @Builder
    public static class StationNode {
        private String stationName;
        private String scheduledArrival;
        private String scheduledDeparture;
        private String actualArrival;
        private String actualDeparture;
        private double distance;
        private int dayOffset;
        private boolean isPassed;   // Already left behind
        private boolean isCurrent;  // Train is exactly here now
    }
}

package com.railway.booking.service;

import com.railway.booking.dao.LiveStatusSnapshotDAO;
import com.railway.booking.dao.TrainDAO;
import com.railway.booking.dto.TrainStatusDTO;
import com.railway.booking.model.LiveStatusSnapshot;
import com.railway.booking.model.Train;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainTrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrainTrackingService.class);

    private final TrainDAO trainDAO;
    private final LiveStatusSnapshotDAO liveStatusSnapshotDAO;
    private final LiveStatusProvider liveStatusProvider;

    @Value("${live.status.cache.ttl-minutes:15}")
    private long cacheTtlMinutes;

    public TrainTrackingService(TrainDAO trainDAO,
                                LiveStatusSnapshotDAO liveStatusSnapshotDAO,
                                LiveStatusProvider liveStatusProvider) {
        this.trainDAO = trainDAO;
        this.liveStatusSnapshotDAO = liveStatusSnapshotDAO;
        this.liveStatusProvider = liveStatusProvider;
    }

    /**
     * Attempts to fetch Live Train Status.
     * Pipeline: live provider -> recent cache -> deterministic fallback.
     */
    public TrainStatusDTO getLiveStatus(String trainNumber, LocalDate journeyDate) {
        Train train = trainDAO.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new RuntimeException("Train not found with number: " + trainNumber));

        Optional<TrainStatusDTO> live = liveStatusProvider.fetchLiveStatus(train, journeyDate);
        if (live.isPresent()) {
            TrainStatusDTO dto = live.get();
            dto.setDataSource("LIVE");
            cacheSnapshot(trainNumber, journeyDate, dto, "LIVE");
            return dto;
        }

        Optional<TrainStatusDTO> cached = getRecentCachedStatus(trainNumber, journeyDate);
        if (cached.isPresent()) {
            TrainStatusDTO dto = cached.get();
            dto.setDataSource("CACHED");
            return dto;
        }

        TrainStatusDTO scheduled = simulateDeterministicStatus(train, journeyDate);
        scheduled.setDataSource("SCHEDULED");
        cacheSnapshot(trainNumber, journeyDate, scheduled, "SCHEDULED");
        return scheduled;
    }

    public boolean refreshLiveStatus(String trainNumber, LocalDate journeyDate) {
        Train train = trainDAO.findByTrainNumber(trainNumber).orElse(null);
        if (train == null) {
            return false;
        }
        Optional<TrainStatusDTO> live = liveStatusProvider.fetchLiveStatus(train, journeyDate);
        if (live.isEmpty()) {
            return false;
        }
        TrainStatusDTO dto = live.get();
        dto.setDataSource("LIVE");
        cacheSnapshot(trainNumber, journeyDate, dto, "LIVE");
        return true;
    }

    private Optional<TrainStatusDTO> getRecentCachedStatus(String trainNumber, LocalDate journeyDate) {
        return liveStatusSnapshotDAO
                .findTopByTrainNumberAndJourneyDateOrderByFetchedAtDesc(trainNumber, journeyDate)
                .filter(snapshot -> snapshot.getFetchedAt() != null
                        && snapshot.getFetchedAt().isAfter(LocalDateTime.now().minusMinutes(cacheTtlMinutes)))
                .map(LiveStatusSnapshot::getPayload);
    }

    private void cacheSnapshot(String trainNumber, LocalDate journeyDate, TrainStatusDTO payload, String source) {
        try {
            LiveStatusSnapshot snapshot = new LiveStatusSnapshot();
            snapshot.setTrainNumber(trainNumber);
            snapshot.setJourneyDate(journeyDate);
            snapshot.setFetchedAt(LocalDateTime.now());
            snapshot.setSource(source);
            snapshot.setPayload(payload);
            liveStatusSnapshotDAO.save(snapshot);
        } catch (Exception e) {
            log.warn("Failed to cache live status snapshot for train {}: {}", trainNumber, e.getMessage());
        }
    }

    private TrainStatusDTO simulateDeterministicStatus(Train train, LocalDate journeyDate) {
        LocalTime now = LocalTime.now();
        
        TrainStatusDTO dto = TrainStatusDTO.builder()
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .journeyDate(journeyDate.toString())
                .totalDistanceKm(train.getDistanceKm())
                .build();

        long minutesSinceMidnight = now.toSecondOfDay() / 60;
        
        List<TrainStatusDTO.StationNode> timeline = new ArrayList<>();
        TrainStatusDTO.StationNode previousNode = null;
        
        String currentMsg = "Not Started";
        String currentStation = train.getSource();
        double currentDist = 0;
        boolean term = false;

        // If the train doesn't run today, we can just reject or show mock. Assuming mock is fine.
        
        for (int i = 0; i < train.getRouteStops().size(); i++) {
            Train.RouteStop stop = train.getRouteStops().get(i);
            
            // Simulating linear passage of time
            LocalTime arr = stop.getArrivalTime() != null ? stop.getArrivalTime() : train.getDepartureTime();
            LocalTime dep = stop.getDepartureTime() != null ? stop.getDepartureTime() : train.getArrivalTime();
            
            long nodeArrMins = arr.toSecondOfDay() / 60;
            if (stop.getDayOffset() > 1) {
                nodeArrMins += (stop.getDayOffset() - 1) * 24 * 60;
            }

            boolean isPassed = nodeArrMins < minutesSinceMidnight;
            boolean isCurrent = false;

            if (isPassed) {
                currentStation = stop.getStationName();
                currentDist = stop.getDistanceKmFromOrigin();
                currentMsg = "Departed " + stop.getStationName();
                
                if (i == train.getRouteStops().size() - 1) {
                    currentMsg = "Arrived at Destination: " + stop.getStationName();
                    term = true;
                }
            } else if (previousNode != null && previousNode.isPassed()) {
                // Train is exactly between previous node and this node!
                isCurrent = true;
                currentMsg = "En Route to " + stop.getStationName();
                
                // Interpolate distance
                double distDiff = stop.getDistanceKmFromOrigin() - currentDist;
                // Add a mock random progression
                currentDist += (distDiff * 0.5); 
            }

            TrainStatusDTO.StationNode node = TrainStatusDTO.StationNode.builder()
                    .stationName(stop.getStationName())
                    .scheduledArrival(arr.toString())
                    .scheduledDeparture(dep.toString())
                    .actualArrival(arr.toString()) // Perfect simulation, 0 delay
                    .actualDeparture(dep.toString())
                    .distance(stop.getDistanceKmFromOrigin())
                    .dayOffset(stop.getDayOffset())
                    .isPassed(isPassed)
                    .isCurrent(isCurrent)
                    .build();
            
            timeline.add(node);
            previousNode = node;
        }
        
        dto.setCurrentStationName(currentStation);
        dto.setCurrentStatusMessage(currentMsg);
        dto.setCoveredDistanceKm(currentDist);
        dto.setCurrentDelayMinutes(0);
        dto.setTerminated(term);
        dto.setTimeline(timeline);
        
        return dto;
    }
}

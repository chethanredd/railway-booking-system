package com.railway.booking.service;

import com.railway.booking.dto.TrainStatusDTO;
import com.railway.booking.model.Train;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class IndianRailApiLiveStatusProvider implements LiveStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(IndianRailApiLiveStatusProvider.class);

    @Value("${indianrailapi.key:}")
    private String indianRailApiKey;

    @Value("${indianrailapi.base-url:https://indianrailapi.com}")
    private String baseUrl;

    @Override
    public Optional<TrainStatusDTO> fetchLiveStatus(Train train, LocalDate journeyDate) {
        if (indianRailApiKey == null || indianRailApiKey.isBlank() || "YOUR_API_KEY_HERE".equals(indianRailApiKey)) {
            return Optional.empty();
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String dateStr = journeyDate.toString().replace("-", "");
                String url = baseUrl + "/api/v2/livetrainstatus/apikey/"
                    + indianRailApiKey
                    + "/trainnumber/"
                    + train.getTrainNumber()
                    + "/date/"
                    + dateStr
                    + "/";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return Optional.empty();
            }

            JSONObject root = new JSONObject(response.getBody());
            if (root.has("response_code") && root.optInt("response_code", 0) != 200) {
                return Optional.empty();
            }

            String statusMessage = firstNonBlank(root,
                    "position",
                    "status",
                    "current_status",
                    "status_message",
                    "message");
            if (statusMessage == null) {
                statusMessage = "Live status available";
            }

            String currentStationName = firstNonBlank(root,
                    "current_station_name",
                    "position",
                    "current_station"
            );

            int delay = root.optInt("delay", root.optInt("current_delay", 0));
            double coveredDistance = root.optDouble("distance_from_source",
                    root.optDouble("current_station_dist", 0.0));

            List<TrainStatusDTO.StationNode> timeline = parseRouteTimeline(root, train, currentStationName);

            boolean terminated = statusMessage.toLowerCase().contains("arrived")
                    && statusMessage.toLowerCase().contains("destination");

            TrainStatusDTO dto = TrainStatusDTO.builder()
                    .trainNumber(train.getTrainNumber())
                    .trainName(train.getTrainName())
                    .journeyDate(journeyDate.toString())
                    .currentStatusMessage(statusMessage)
                    .currentStationName(currentStationName != null ? currentStationName : train.getSource())
                    .totalDistanceKm(train.getDistanceKm())
                    .coveredDistanceKm(Math.max(0.0, coveredDistance))
                    .currentDelayMinutes(Math.max(0, delay))
                    .isTerminated(terminated)
                    .timeline(timeline)
                    .build();

            return Optional.of(dto);
        } catch (Exception e) {
            log.warn("Live provider fetch failed for train {}: {}", train.getTrainNumber(), e.getMessage());
            return Optional.empty();
        }
    }

    private List<TrainStatusDTO.StationNode> parseRouteTimeline(JSONObject root, Train train, String currentStationName) {
        List<TrainStatusDTO.StationNode> nodes = new ArrayList<>();
        JSONArray route = root.optJSONArray("route");

        if (route != null && !route.isEmpty()) {
            for (int i = 0; i < route.length(); i++) {
                JSONObject stop = route.optJSONObject(i);
                if (stop == null) {
                    continue;
                }
                String stationName = firstNonBlank(stop,
                        "station_name",
                        "name",
                        "station"
                );
                String status = stop.optString("status", "").toLowerCase();
                boolean isCurrent = stationName != null
                        && currentStationName != null
                        && stationName.equalsIgnoreCase(currentStationName);
                boolean isPassed = status.contains("depart") || status.contains("arriv") || status.contains("passed");

                nodes.add(TrainStatusDTO.StationNode.builder()
                        .stationName(stationName != null ? stationName : "Unknown")
                        .scheduledArrival(stop.optString("scharr", stop.optString("scheduled_arrival", "--")))
                        .scheduledDeparture(stop.optString("schdep", stop.optString("scheduled_departure", "--")))
                        .actualArrival(stop.optString("actarr", stop.optString("actual_arrival", "--")))
                        .actualDeparture(stop.optString("actdep", stop.optString("actual_departure", "--")))
                        .distance(stop.optDouble("distance", stop.optDouble("distance_from_source", 0.0)))
                        .dayOffset(stop.optInt("day", 1))
                        .isPassed(isPassed)
                        .isCurrent(isCurrent)
                        .build());
            }
        }

        if (!nodes.isEmpty()) {
            return nodes;
        }

        // Fallback timeline from static route matrix when provider route details are missing.
        if (train.getRouteStops() != null) {
            for (Train.RouteStop stop : train.getRouteStops()) {
                boolean isCurrent = stop.getStationName() != null
                        && currentStationName != null
                        && stop.getStationName().equalsIgnoreCase(currentStationName);
                nodes.add(TrainStatusDTO.StationNode.builder()
                        .stationName(stop.getStationName())
                        .scheduledArrival(stop.getArrivalTime() != null ? stop.getArrivalTime().toString() : "--")
                        .scheduledDeparture(stop.getDepartureTime() != null ? stop.getDepartureTime().toString() : "--")
                        .actualArrival("--")
                        .actualDeparture("--")
                        .distance(stop.getDistanceKmFromOrigin())
                        .dayOffset(stop.getDayOffset())
                        .isPassed(false)
                        .isCurrent(isCurrent)
                        .build());
            }
        }

        return nodes;
    }

    private String firstNonBlank(JSONObject json, String... keys) {
        for (String key : keys) {
            Object value = json.opt(key);
            if (value == null) {
                continue;
            }
            if (value instanceof JSONObject nested) {
                String nestedValue = firstNonBlank(nested,
                        "current_station_name",
                        "status",
                        "station_name",
                        "message");
                if (nestedValue != null && !nestedValue.isBlank()) {
                    return nestedValue;
                }
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }
}

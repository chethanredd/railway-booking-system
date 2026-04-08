package com.railway.booking.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.core.JsonParser;
import com.railway.booking.dao.TrainDAO;
import com.railway.booking.model.Train;
import com.mongodb.MongoTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DatasetImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatasetImportRunner.class);

    private final TrainDAO trainDAO;
    private final ObjectMapper objectMapper;

    @Value("${app.dataset.import.enabled:false}")
    private boolean importEnabled;

    @Value("${app.dataset.import.trains-file:data/kaggle/trains.json}")
    private String trainsFile;

    @Value("${app.dataset.import.schedules-file:data/kaggle/schedules.json}")
    private String schedulesFile;

    public DatasetImportRunner(TrainDAO trainDAO, ObjectMapper objectMapper) {
        this.trainDAO = trainDAO;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (!importEnabled) {
            return;
        }

        File trains = new File(trainsFile);
        File schedules = new File(schedulesFile);
        if (!trains.exists() || !schedules.exists()) {
            log.warn("Dataset import enabled but files not found. trains={}, schedules={}", trains.getPath(), schedules.getPath());
            return;
        }

        try {
            Map<String, List<Train.RouteStop>> routeStopsByTrain = loadRouteStopsByTrain(schedules);
            int updated = importTrains(trains, routeStopsByTrain);
            log.info("Dataset import completed. Upserted {} trains from {}", updated, trains.getPath());
        } catch (IOException | DataAccessException | MongoTimeoutException ex) {
            log.error("Dataset import failed: {}", ex.getMessage());
        }
    }

    private Map<String, List<Train.RouteStop>> loadRouteStopsByTrain(File schedules) throws IOException {
        Map<String, List<Train.RouteStop>> grouped = new HashMap<>();

        ObjectReader reader = objectMapper.readerFor(JsonNode.class);
        try (JsonParser parser = objectMapper.getFactory().createParser(schedules);
             MappingIterator<JsonNode> it = reader.readValues(parser)) {
            while (it.hasNext()) {
                JsonNode n = it.next();
                String trainNumber = text(n, "train_number");
                String stationName = text(n, "station_name");
                if (isBlank(trainNumber) || isBlank(stationName)) {
                    continue;
                }

                Train.RouteStop stop = new Train.RouteStop();
                stop.setStationName(stationName);
                stop.setArrivalTime(parseTime(text(n, "arrival")));
                stop.setDepartureTime(parseTime(text(n, "departure")));
                stop.setDayOffset(dayOffset(n.path("day").asInt(1)));
                stop.setDistanceKmFromOrigin(0);

                grouped.computeIfAbsent(trainNumber, k -> new ArrayList<>()).add(stop);
            }
        }

        Comparator<Train.RouteStop> byTime = Comparator
            .comparingInt(Train.RouteStop::getDayOffset)
            .thenComparing(r -> Optional.ofNullable(r.getDepartureTime()).orElse(LocalTime.MAX))
            .thenComparing(r -> Optional.ofNullable(r.getArrivalTime()).orElse(LocalTime.MAX));

        for (List<Train.RouteStop> stops : grouped.values()) {
            stops.sort(byTime);
            for (int i = 0; i < stops.size(); i++) {
                stops.get(i).setDistanceKmFromOrigin(i * 10.0);
            }
        }

        return grouped;
    }

    private int importTrains(File trains, Map<String, List<Train.RouteStop>> routeStopsByTrain) throws IOException {
        JsonNode root = objectMapper.readTree(trains);
        JsonNode features = root.path("features");
        if (!features.isArray()) {
            return 0;
        }

        int upserted = 0;
        for (JsonNode feature : features) {
            JsonNode props = feature.path("properties");
            String trainNumber = text(props, "number");
            if (isBlank(trainNumber)) {
                continue;
            }

            Train train = trainDAO.findByTrainNumber(trainNumber).orElseGet(Train::new);
            train.setTrainNumber(trainNumber);
            train.setTrainName(textOrDefault(props, "name", "Train " + trainNumber));
            train.setSource(textOrDefault(props, "from_station_name", "Unknown"));
            train.setDestination(textOrDefault(props, "to_station_name", "Unknown"));
            train.setDepartureTime(parseTime(text(props, "departure")));
            train.setArrivalTime(parseTime(text(props, "arrival")));

            int durationMinutes = props.path("duration_h").asInt(0) * 60 + props.path("duration_m").asInt(0);
            train.setDurationMinutes(durationMinutes);
            train.setDistanceKm(props.path("distance").asDouble(0));
            train.setTrainType(mapTrainType(text(props, "type")));
            train.setRunningDays("MON,TUE,WED,THU,FRI,SAT,SUN");
            train.setActive(true);

            List<Train.RouteStop> stops = routeStopsByTrain.getOrDefault(trainNumber, List.of());
            train.setRouteStops(new ArrayList<>(stops));

            trainDAO.save(train);
            upserted++;
        }
        return upserted;
    }

    private Train.TrainType mapTrainType(String rawType) {
        if (rawType == null) {
            return Train.TrainType.SUPERFAST_EXPRESS;
        }
        String v = rawType.trim().toUpperCase();
        if (v.contains("RAJDHANI")) return Train.TrainType.RAJDHANI;
        if (v.contains("SHATABDI")) return Train.TrainType.SHATABDI;
        if (v.contains("DURONTO")) return Train.TrainType.DURONTO;
        if (v.contains("VANDE")) return Train.TrainType.VANDE_BHARAT;
        if (v.contains("GARIB")) return Train.TrainType.GARIB_RATH;
        if (v.contains("JAN SHATABDI") || v.contains("JANSHATABDI")) return Train.TrainType.JAN_SHATABDI;
        if (v.contains("PASSENGER") || v.contains("DEMU") || v.contains("MEMU")) return Train.TrainType.PASSENGER;
        if (v.contains("MAIL") || v.contains("EXPRESS")) return Train.TrainType.MAIL_EXPRESS;
        return Train.TrainType.SUPERFAST_EXPRESS;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String s = value.asText();
        if (isBlank(s) || "None".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return isBlank(value) ? fallback : value;
    }

    private int dayOffset(int rawDay) {
        return rawDay <= 0 ? 1 : rawDay;
    }

    private LocalTime parseTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

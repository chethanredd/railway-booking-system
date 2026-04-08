package com.railway.booking.service;

import com.railway.booking.dao.SeatDAO;
import com.railway.booking.dao.TrainDAO;
import com.railway.booking.dto.TrainSearchResult;
import com.railway.booking.model.Seat;
import com.railway.booking.model.Train;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * SERVICE: TrainService — Search, availability (as in MVC diagram)
 */
@Service
public class TrainService {

    private static final Logger log = LoggerFactory.getLogger(TrainService.class);

    private final TrainDAO trainDAO;
    private final SeatDAO seatDAO;
    private final FareService fareService;

    public TrainService(TrainDAO trainDAO, SeatDAO seatDAO, FareService fareService) {
        this.trainDAO = trainDAO;
        this.seatDAO = seatDAO;
        this.fareService = fareService;
    }

    public List<TrainSearchResult> searchTrains(String source, String destination, LocalDate date) {
        log.info("Searching trains: {} → {} on {}", source, destination, date);
        String normalizedSource = normalizeStation(source);
        String normalizedDestination = normalizeStation(destination);

        List<Train> trains = trainDAO.findByActive(true);
        return trains.stream()
                .filter(t -> t.runsOn(date))
                .filter(t -> matchesStationPairInRoute(t, normalizedSource, normalizedDestination))
                .map(t -> buildSearchResult(t, date))
                .collect(Collectors.toList());
    }

    private boolean matchesStationPairInRoute(Train train, String source, String destination) {
        List<Train.RouteStop> routeStops = train.getRouteStops();
        if (routeStops != null && !routeStops.isEmpty()) {
            int sourceIdx = -1;
            int destinationIdx = -1;

            for (int i = 0; i < routeStops.size(); i++) {
                String stop = normalizeStation(routeStops.get(i).getStationName());
                if (sourceIdx < 0 && stationMatches(stop, source)) {
                    sourceIdx = i;
                    continue;
                }
                if (sourceIdx >= 0 && stationMatches(stop, destination)) {
                    destinationIdx = i;
                    break;
                }
            }

            if (sourceIdx >= 0 && destinationIdx > sourceIdx) {
                return true;
            }
        }

        String trainSource = normalizeStation(train.getSource());
        String trainDestination = normalizeStation(train.getDestination());
        return stationMatches(trainSource, source) && stationMatches(trainDestination, destination);
    }

    private boolean stationMatches(String storedStation, String userStation) {
        if (storedStation == null || storedStation.isBlank() || userStation == null || userStation.isBlank()) {
            return false;
        }
        return storedStation.contains(userStation) || userStation.contains(storedStation);
    }

    private String normalizeStation(String station) {
        if (station == null) {
            return "";
        }
        return station
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private TrainSearchResult buildSearchResult(Train train, LocalDate date) {
        return TrainSearchResult.builder()
                .train(train)
                .journeyDate(date)
                .slAvailable(seatDAO.countAvailableSeats(train.getTrainId(), Seat.TravelClass.SLEEPER))
                .ac3Available(seatDAO.countAvailableSeats(train.getTrainId(), Seat.TravelClass.AC_3_TIER))
                .ac2Available(seatDAO.countAvailableSeats(train.getTrainId(), Seat.TravelClass.AC_2_TIER))
                .ac1Available(seatDAO.countAvailableSeats(train.getTrainId(), Seat.TravelClass.AC_1_TIER))
                .ccAvailable(seatDAO.countAvailableSeats(train.getTrainId(), Seat.TravelClass.CC))
                .slFare(fareService.calculateFare(train, Seat.TravelClass.SLEEPER))
                .ac3Fare(fareService.calculateFare(train, Seat.TravelClass.AC_3_TIER))
                .ac2Fare(fareService.calculateFare(train, Seat.TravelClass.AC_2_TIER))
                .ac1Fare(fareService.calculateFare(train, Seat.TravelClass.AC_1_TIER))
                .ccFare(fareService.calculateFare(train, Seat.TravelClass.CC))
                .build();
    }

    public Train getTrainById(String id) {
        return trainDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Train not found: " + id));
    }

    public Train getTrainByNumber(String number) {
        return trainDAO.findByTrainNumber(number)
                .orElseThrow(() -> new IllegalArgumentException("Train not found: " + number));
    }

    public List<Train> getAllTrains() {
        return trainDAO.findAll();
    }

    public List<Train> searchByQuery(String query) {
        return trainDAO.searchTrains(query);
    }

    public List<String> getStationSuggestions() {
        Set<String> suggestions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Train train : trainDAO.findByActive(true)) {
            addIfPresent(suggestions, train.getSource());
            addIfPresent(suggestions, train.getDestination());

            if (train.getRouteStops() != null) {
                for (Train.RouteStop stop : train.getRouteStops()) {
                    addIfPresent(suggestions, stop.getStationName());
                }
            }
        }

        return new ArrayList<>(suggestions);
    }

    public List<String> getTrainQuerySuggestions() {
        Set<String> suggestions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Train train : trainDAO.findByActive(true)) {
            addIfPresent(suggestions, train.getTrainNumber());
            addIfPresent(suggestions, train.getTrainName());
            if (train.getTrainNumber() != null && train.getTrainName() != null) {
                suggestions.add(train.getTrainNumber() + " - " + train.getTrainName());
            }
        }

        return new ArrayList<>(suggestions);
    }

    public String resolveTrainNumberForStatusQuery(String query) {
        String input = query == null ? "" : query.trim();
        if (input.isBlank()) {
            throw new IllegalArgumentException("Please enter a train name or number.");
        }

        // Support selection values like "12951 - Mumbai Rajdhani Express"
        int dash = input.indexOf(" - ");
        if (dash > 0) {
            String possibleNumber = input.substring(0, dash).trim();
            if (isSimpleTrainCode(possibleNumber) && trainDAO.findByTrainNumber(possibleNumber).isPresent()) {
                return possibleNumber;
            }
        }

        if (isSimpleTrainCode(input) && trainDAO.findByTrainNumber(input).isPresent()) {
            return input;
        }

        String normalizedInput = normalizeStation(input);
        List<Train> matches = trainDAO.findByActive(true).stream()
                .filter(t -> {
                    String number = normalizeStation(t.getTrainNumber());
                    String name = normalizeStation(t.getTrainName());
                    return number.equals(normalizedInput)
                            || name.equals(normalizedInput)
                            || number.contains(normalizedInput)
                            || name.contains(normalizedInput);
                })
                .sorted(Comparator.comparingInt(t -> rankMatch(t, normalizedInput)))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No active train found for the given name/number.");
        }

        return matches.get(0).getTrainNumber();
    }

    private int rankMatch(Train train, String normalizedInput) {
        String number = normalizeStation(train.getTrainNumber());
        String name = normalizeStation(train.getTrainName());
        if (number.equals(normalizedInput) || name.equals(normalizedInput)) {
            return 0;
        }
        if (number.startsWith(normalizedInput) || name.startsWith(normalizedInput)) {
            return 1;
        }
        return 2;
    }

    private boolean isSimpleTrainCode(String input) {
        return input != null && input.matches("^[A-Za-z0-9]{1,10}$");
    }

    private void addIfPresent(Set<String> set, String value) {
        if (value == null) {
            return;
        }
        String cleaned = value.trim();
        if (!cleaned.isBlank()) {
            set.add(cleaned);
        }
    }

    public Train addTrain(Train train) {
        if (trainDAO.existsByTrainNumber(train.getTrainNumber())) {
            throw new IllegalArgumentException("Train number already exists: " + train.getTrainNumber());
        }
        return trainDAO.save(train);
    }

    public Train updateTrain(Train train) {
        trainDAO.findById(train.getTrainId())
                .orElseThrow(() -> new IllegalArgumentException("Train not found"));
        return trainDAO.save(train);
    }

    public void deleteTrain(String id) {
        trainDAO.deleteById(id);
        log.warn("Train deleted: id={}", id);
    }
}

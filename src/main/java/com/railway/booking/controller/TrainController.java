package com.railway.booking.controller;

import com.railway.booking.dto.TrainSearchResult;
import com.railway.booking.model.Seat;
import com.railway.booking.model.Train;
import com.railway.booking.service.TrainService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CONTROLLER: TrainController — Search, view trains (as in MVC diagram)
 * Handles SearchPanel view.
 */
@Controller
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    /** Home page — renders search panel */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusDays(120));
        model.addAttribute("popularRoutes", getPopularRoutes());
        model.addAttribute("stationSuggestions", trainService.getStationSuggestions());
        model.addAttribute("trainQueries", trainService.getTrainQuerySuggestions());
        return "index";
    }

    /** Dashboard — after login */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("popularRoutes", getPopularRoutes());
        model.addAttribute("trainTypes", Train.TrainType.values());
        return "dashboard";
    }

    /** Train search results */
    @GetMapping("/trains/search")
    public String searchTrains(@RequestParam String source,
                               @RequestParam String destination,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(defaultValue = "1") int passengers,
                               Model model) {
        String sourceTrimmed = source == null ? "" : source.trim();
        String destinationTrimmed = destination == null ? "" : destination.trim();

        if (sourceTrimmed.isBlank() || destinationTrimmed.isBlank()) {
            model.addAttribute("error", "Source and destination are required.");
            populateHomeModel(model);
            return "index";
        }
        if (sourceTrimmed.equalsIgnoreCase(destinationTrimmed)) {
            model.addAttribute("error", "Source and destination cannot be the same.");
            populateHomeModel(model);
            return "index";
        }
        if (passengers < 1 || passengers > 6) {
            model.addAttribute("error", "Passengers must be between 1 and 6.");
            populateHomeModel(model);
            return "index";
        }
        if (date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusDays(120))) {
            model.addAttribute("error", "Journey date must be within the next 120 days.");
            populateHomeModel(model);
            return "index";
        }

        List<TrainSearchResult> results = trainService.searchTrains(sourceTrimmed, destinationTrimmed, date);

        model.addAttribute("results", results);
        model.addAttribute("source", sourceTrimmed);
        model.addAttribute("destination", destinationTrimmed);
        model.addAttribute("date", date);
        model.addAttribute("passengers", passengers);
        model.addAttribute("travelClasses", Seat.TravelClass.values());
        model.addAttribute("stationSuggestions", trainService.getStationSuggestions());
        return "search-results";
    }

    /** Single train detail */
    @GetMapping("/trains/{id}")
    public String trainDetail(@PathVariable String id, Model model) {
        Train train = trainService.getTrainById(id);
        model.addAttribute("train", train);
        model.addAttribute("travelClasses", Seat.TravelClass.values());
        return "train-detail";
    }

    /** PNR Status check (public) */
    @GetMapping("/pnr-status")
    public String pnrStatusPage() {
        return "pnr-status";
    }

    private List<String[]> getPopularRoutes() {
        return List.of(
            new String[]{"New Delhi", "Mumbai Central"},
            new String[]{"Mumbai Central", "Howrah Junction"},
            new String[]{"Chennai Central", "Bangalore City"},
            new String[]{"New Delhi", "Kolkata"},
            new String[]{"Ahmedabad", "Mumbai Central"},
            new String[]{"Hyderabad", "Chennai Central"}
        );
    }

    private void populateHomeModel(Model model) {
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusDays(120));
        model.addAttribute("popularRoutes", getPopularRoutes());
        model.addAttribute("stationSuggestions", trainService.getStationSuggestions());
        model.addAttribute("trainQueries", trainService.getTrainQuerySuggestions());
    }
}

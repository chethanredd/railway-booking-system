package com.railway.booking.controller;

import com.railway.booking.dto.TrainStatusDTO;
import com.railway.booking.service.TrainService;
import com.railway.booking.service.TrainTrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/trains")
public class TrainStatusController {

    private final TrainTrackingService trainTrackingService;
    private final TrainService trainService;

    public TrainStatusController(TrainTrackingService trainTrackingService, TrainService trainService) {
        this.trainTrackingService = trainTrackingService;
        this.trainService = trainService;
    }

    @GetMapping("/status")
    public String getLiveTrainStatus(
            @RequestParam String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        String query = q == null ? "" : q.trim();
        if (query.isBlank() || query.length() > 100) {
            model.addAttribute("error", "Please enter a valid train name or number.");
            return "live-status";
        }
        
        // Default to today if date is missing
        if (date == null) {
            date = LocalDate.now();
        }
        if (date.isBefore(LocalDate.now().minusDays(1)) || date.isAfter(LocalDate.now().plusDays(7))) {
            model.addAttribute("error", "Date must be between yesterday and the next 7 days.");
            return "live-status";
        }

        try {
            String resolvedTrainNumber = trainService.resolveTrainNumberForStatusQuery(query);
            TrainStatusDTO status = trainTrackingService.getLiveStatus(resolvedTrainNumber, date);
            model.addAttribute("status", status);
            return "live-status";
        } catch (Exception e) {
            model.addAttribute("error", "Train not found or inactive. Please check the train name/number.");
            return "live-status"; 
        }
    }
}

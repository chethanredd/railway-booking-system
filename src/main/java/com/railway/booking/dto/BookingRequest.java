package com.railway.booking.dto;

import com.railway.booking.model.Seat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/** DTO for the booking form — submitted by the user from BookingView */
public class BookingRequest {

    @NotNull(message = "Train ID is required")
    private String trainId;

    @NotNull(message = "Journey date is required")
    @FutureOrPresent(message = "Journey date cannot be in the past")
    private LocalDate journeyDate;

    @NotBlank(message = "Boarding station is required")
    private String boardingStation;

    @NotBlank(message = "Destination station is required")
    private String destinationStation;

    @NotNull(message = "Travel class is required")
    private Seat.TravelClass travelClass;

    @NotNull(message = "At least one passenger is required")
    @Size(min = 1, max = 6, message = "You can book between 1 and 6 passengers")
    private List<PassengerDto> passengers;

    // Payment method is now optional - selected AFTER booking confirmation
    private String paymentMethod;

    // ── Getters / Setters ─────────────────────────────────────────
    public String getTrainId() { return trainId; }
    public void setTrainId(String trainId) { this.trainId = trainId; }
    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }
    public String getBoardingStation() { return boardingStation; }
    public void setBoardingStation(String boardingStation) { this.boardingStation = boardingStation; }
    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }
    public Seat.TravelClass getTravelClass() { return travelClass; }
    public void setTravelClass(Seat.TravelClass travelClass) { this.travelClass = travelClass; }
    public List<PassengerDto> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDto> passengers) { this.passengers = passengers; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public static class PassengerDto {
        @NotBlank(message = "Passenger name is required")
        @Size(min = 2, max = 100, message = "Passenger name must be between 2 and 100 characters")
        private String name;

        @Min(value = 1, message = "Age must be at least 1")
        @Max(value = 125, message = "Age must be at most 125")
        private int age;

        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE or OTHER")
        private String gender;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }
}

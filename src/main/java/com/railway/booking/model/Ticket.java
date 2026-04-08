package com.railway.booking.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MODEL: Ticket — PNR, status, fare (as in MVC diagram)
 */
@Document(collection = "tickets")
public class Ticket {

    @Id
    private String ticketId;

    private String pnr;

    @DBRef
    private User user;

    @DBRef
    private Train train;

    private LocalDate journeyDate;

    private String boardingStation;

    private String destinationStation;

    private Seat.TravelClass travelClass;

    private TicketStatus status = TicketStatus.CONFIRMED;

    private double totalFare;

    private double refundAmount;

    // Embedded Passengers Array
    private List<Passenger> passengers = new ArrayList<>();

    @DBRef
    private Payment payment;

    private LocalDateTime bookedAt = LocalDateTime.now();

    private LocalDateTime cancelledAt;

    public void generatePnr() {
        if (this.pnr == null) {
            this.pnr = String.valueOf((long)(Math.random() * 9_000_000_000L) + 1_000_000_000L);
        }
    }

    public enum TicketStatus {
        CONFIRMED, WAITLISTED, RAC, CANCELLED, PARTIALLY_CANCELLED
    }

    // ── Constructors ──────────────────────────────────────────────
    public Ticket() {
        generatePnr();
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }
    public String getBoardingStation() { return boardingStation; }
    public void setBoardingStation(String boardingStation) { this.boardingStation = boardingStation; }
    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }
    public Seat.TravelClass getTravelClass() { return travelClass; }
    public void setTravelClass(Seat.TravelClass travelClass) { this.travelClass = travelClass; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }
    public List<Passenger> getPassengers() { return passengers; }
    public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String ticketId; private User user; private Train train; private LocalDate journeyDate;
        private String boardingStation; private String destinationStation;
        private Seat.TravelClass travelClass; private TicketStatus status = TicketStatus.CONFIRMED;
        private double totalFare; private List<Passenger> passengers = new ArrayList<>();
        public Builder ticketId(String v)               { this.ticketId=v; return this; }
        public Builder user(User v)                     { this.user=v; return this; }
        public Builder train(Train v)                   { this.train=v; return this; }
        public Builder journeyDate(LocalDate v)         { this.journeyDate=v; return this; }
        public Builder boardingStation(String v)        { this.boardingStation=v; return this; }
        public Builder destinationStation(String v)     { this.destinationStation=v; return this; }
        public Builder travelClass(Seat.TravelClass v)  { this.travelClass=v; return this; }
        public Builder status(TicketStatus v)           { this.status=v; return this; }
        public Builder totalFare(double v)              { this.totalFare=v; return this; }
        public Ticket build() {
            Ticket t = new Ticket();
            t.ticketId=ticketId; t.user=user; t.train=train; t.journeyDate=journeyDate;
            t.boardingStation=boardingStation; t.destinationStation=destinationStation;
            t.travelClass=travelClass; t.status=status; t.totalFare=totalFare;
            t.passengers=passengers;
            return t;
        }
    }
}

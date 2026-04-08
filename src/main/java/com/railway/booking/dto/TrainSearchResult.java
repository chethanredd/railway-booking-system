package com.railway.booking.dto;

import com.railway.booking.model.Seat;
import com.railway.booking.model.Train;

import java.time.LocalDate;

/** DTO carrying search results — passed from TrainController to SearchPanel view */
public class TrainSearchResult {

    private Train train;
    private LocalDate journeyDate;

    private int slAvailable;
    private int ac3Available;
    private int ac2Available;
    private int ac1Available;
    private int ccAvailable;

    private double slFare;
    private double ac3Fare;
    private double ac2Fare;
    private double ac1Fare;
    private double ccFare;

    public TrainSearchResult() {}

    public int getAvailabilityFor(Seat.TravelClass cls) {
        return switch (cls) {
            case SLEEPER    -> slAvailable;
            case AC_3_TIER  -> ac3Available;
            case AC_2_TIER  -> ac2Available;
            case AC_1_TIER  -> ac1Available;
            case CC         -> ccAvailable;
            default         -> 0;
        };
    }

    public double getFareFor(Seat.TravelClass cls) {
        return switch (cls) {
            case SLEEPER    -> slFare;
            case AC_3_TIER  -> ac3Fare;
            case AC_2_TIER  -> ac2Fare;
            case AC_1_TIER  -> ac1Fare;
            case CC         -> ccFare;
            default         -> 0;
        };
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }
    public int getSlAvailable() { return slAvailable; }
    public void setSlAvailable(int slAvailable) { this.slAvailable = slAvailable; }
    public int getAc3Available() { return ac3Available; }
    public void setAc3Available(int ac3Available) { this.ac3Available = ac3Available; }
    public int getAc2Available() { return ac2Available; }
    public void setAc2Available(int ac2Available) { this.ac2Available = ac2Available; }
    public int getAc1Available() { return ac1Available; }
    public void setAc1Available(int ac1Available) { this.ac1Available = ac1Available; }
    public int getCcAvailable() { return ccAvailable; }
    public void setCcAvailable(int ccAvailable) { this.ccAvailable = ccAvailable; }
    public double getSlFare() { return slFare; }
    public void setSlFare(double slFare) { this.slFare = slFare; }
    public double getAc3Fare() { return ac3Fare; }
    public void setAc3Fare(double ac3Fare) { this.ac3Fare = ac3Fare; }
    public double getAc2Fare() { return ac2Fare; }
    public void setAc2Fare(double ac2Fare) { this.ac2Fare = ac2Fare; }
    public double getAc1Fare() { return ac1Fare; }
    public void setAc1Fare(double ac1Fare) { this.ac1Fare = ac1Fare; }
    public double getCcFare() { return ccFare; }
    public void setCcFare(double ccFare) { this.ccFare = ccFare; }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Train train; private LocalDate journeyDate;
        private int slAvailable, ac3Available, ac2Available, ac1Available, ccAvailable;
        private double slFare, ac3Fare, ac2Fare, ac1Fare, ccFare;
        public Builder train(Train v)         { this.train=v; return this; }
        public Builder journeyDate(LocalDate v){ this.journeyDate=v; return this; }
        public Builder slAvailable(int v)     { this.slAvailable=v; return this; }
        public Builder ac3Available(int v)    { this.ac3Available=v; return this; }
        public Builder ac2Available(int v)    { this.ac2Available=v; return this; }
        public Builder ac1Available(int v)    { this.ac1Available=v; return this; }
        public Builder ccAvailable(int v)     { this.ccAvailable=v; return this; }
        public Builder slFare(double v)       { this.slFare=v; return this; }
        public Builder ac3Fare(double v)      { this.ac3Fare=v; return this; }
        public Builder ac2Fare(double v)      { this.ac2Fare=v; return this; }
        public Builder ac1Fare(double v)      { this.ac1Fare=v; return this; }
        public Builder ccFare(double v)       { this.ccFare=v; return this; }
        public TrainSearchResult build() {
            TrainSearchResult r = new TrainSearchResult();
            r.train=train; r.journeyDate=journeyDate;
            r.slAvailable=slAvailable; r.ac3Available=ac3Available;
            r.ac2Available=ac2Available; r.ac1Available=ac1Available; r.ccAvailable=ccAvailable;
            r.slFare=slFare; r.ac3Fare=ac3Fare; r.ac2Fare=ac2Fare; r.ac1Fare=ac1Fare; r.ccFare=ccFare;
            return r;
        }
    }
}

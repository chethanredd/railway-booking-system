package com.railway.booking.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MODEL: Train — trainId, schedule, seats, routes (as in MVC diagram)
 */
@Document(collection = "trains")
public class Train {

    @Id
    private String trainId;

    private String trainNumber;
    private String trainName;

    // We can still keep source and destination for quick reference, 
    // but the actual routing is inside routeStops.
    private String source;
    private String destination;

    private LocalTime departureTime;
    private LocalTime arrivalTime;

    /** Comma-separated days: MON,TUE,WED,THU,FRI,SAT,SUN */
    private String runningDays;
    
    private int durationMinutes;
    private double distanceKm;

    private TrainType trainType = TrainType.SUPERFAST_EXPRESS;
    
    private boolean active = true;

    // NEW: Intermediate Station Halts (Route Matrix)
    private List<RouteStop> routeStops = new ArrayList<>();

    public enum TrainType {
        RAJDHANI, SHATABDI, DURONTO, VANDEMATARAM, SUPERFAST_EXPRESS,
        MAIL_EXPRESS, PASSENGER, GARIB_RATH, JAN_SHATABDI, VANDE_BHARAT
    }

    public static class RouteStop {
        private String stationName;
        private LocalTime arrivalTime;
        private LocalTime departureTime;
        private int dayOffset; // 1 for same day, 2 for next day, etc.
        private double distanceKmFromOrigin;

        public RouteStop() {}
        
        public RouteStop(String stationName, LocalTime arrivalTime, LocalTime departureTime, int dayOffset, double distanceKmFromOrigin) {
            this.stationName = stationName;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.dayOffset = dayOffset;
            this.distanceKmFromOrigin = distanceKmFromOrigin;
        }

        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }
        public LocalTime getArrivalTime() { return arrivalTime; }
        public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
        public LocalTime getDepartureTime() { return departureTime; }
        public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
        public int getDayOffset() { return dayOffset; }
        public void setDayOffset(int dayOffset) { this.dayOffset = dayOffset; }
        public double getDistanceKmFromOrigin() { return distanceKmFromOrigin; }
        public void setDistanceKmFromOrigin(double distanceKmFromOrigin) { this.distanceKmFromOrigin = distanceKmFromOrigin; }
    }

    // ── Constructors ──────────────────────────────────────────────
    public Train() {}

    // ── Getters / Setters ─────────────────────────────────────────
    public String getTrainId() { return trainId; }
    public void setTrainId(String trainId) { this.trainId = trainId; }
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getRunningDays() { return runningDays; }
    public void setRunningDays(String runningDays) { this.runningDays = runningDays; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public TrainType getTrainType() { return trainType; }
    public void setTrainType(TrainType trainType) { this.trainType = trainType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<RouteStop> getRouteStops() { return routeStops; }
    public void setRouteStops(List<RouteStop> routeStops) { this.routeStops = routeStops; }

    /** Check if train runs on a given date */
    public boolean runsOn(LocalDate date) {
        String dayCode = date.getDayOfWeek().name().substring(0, 3);
        return runningDays != null && runningDays.toUpperCase().contains(dayCode);
    }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String trainId; private String trainNumber; private String trainName;
        private String source; private String destination;
        private LocalTime departureTime; private LocalTime arrivalTime;
        private String runningDays; private int durationMinutes; private double distanceKm;
        private TrainType trainType = TrainType.SUPERFAST_EXPRESS; private boolean active = true;
        private List<RouteStop> routeStops = new ArrayList<>();
        public Builder trainId(String v)           { this.trainId=v; return this; }
        public Builder trainNumber(String v)     { this.trainNumber=v; return this; }
        public Builder trainName(String v)       { this.trainName=v; return this; }
        public Builder source(String v)          { this.source=v; return this; }
        public Builder destination(String v)     { this.destination=v; return this; }
        public Builder departureTime(LocalTime v){ this.departureTime=v; return this; }
        public Builder arrivalTime(LocalTime v)  { this.arrivalTime=v; return this; }
        public Builder runningDays(String v)     { this.runningDays=v; return this; }
        public Builder durationMinutes(int v)    { this.durationMinutes=v; return this; }
        public Builder distanceKm(double v)      { this.distanceKm=v; return this; }
        public Builder trainType(TrainType v)    { this.trainType=v; return this; }
        public Builder active(boolean v)         { this.active=v; return this; }
        public Builder routeStops(List<RouteStop> v) { this.routeStops=v; return this; }
        public Train build() {
            Train t = new Train();
            t.trainId=trainId; t.trainNumber=trainNumber; t.trainName=trainName;
            t.source=source; t.destination=destination;
            t.departureTime=departureTime; t.arrivalTime=arrivalTime;
            t.runningDays=runningDays; t.durationMinutes=durationMinutes;
            t.distanceKm=distanceKm; t.trainType=trainType; t.active=active;
            t.routeStops=routeStops;
            return t;
        }
    }
}

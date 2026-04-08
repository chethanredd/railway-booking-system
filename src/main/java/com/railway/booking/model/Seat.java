package com.railway.booking.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MODEL: Seat — seatId, class, status (as in MVC diagram)
 */
@Document(collection = "seats")
@CompoundIndex(name = "train_seat_coach_class_idx", def = "{'train.$id': 1, 'seatNumber': 1, 'coachNumber': 1, 'travelClass': 1}", unique = true)
public class Seat {

    @Id
    private String seatId;

    @DBRef
    private Train train;

    private String seatNumber;

    private String coachNumber;

    private TravelClass travelClass;

    private SeatStatus status = SeatStatus.AVAILABLE;

    private String seatType;

    public enum TravelClass {
        SLEEPER("SL", "Sleeper Class"),
        AC_3_TIER("3A", "AC 3 Tier"),
        AC_2_TIER("2A", "AC 2 Tier"),
        AC_1_TIER("1A", "AC First Class"),
        CC("CC", "Chair Car"),
        EC("EC", "Executive Chair Car"),
        GENERAL("GN", "General");

        private final String code;
        private final String displayName;

        TravelClass(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        public String getCode()        { return code; }
        public String getDisplayName() { return displayName; }
    }

    public enum SeatStatus {
        AVAILABLE, BOOKED, WAITLIST, BLOCKED
    }

    // ── Constructors ──────────────────────────────────────────────
    public Seat() {}

    // ── Getters / Setters ─────────────────────────────────────────
    public String getSeatId() { return seatId; }
    public void setSeatId(String seatId) { this.seatId = seatId; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getCoachNumber() { return coachNumber; }
    public void setCoachNumber(String coachNumber) { this.coachNumber = coachNumber; }
    public TravelClass getTravelClass() { return travelClass; }
    public void setTravelClass(TravelClass travelClass) { this.travelClass = travelClass; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Train train; private String seatNumber; private String coachNumber;
        private TravelClass travelClass; private SeatStatus status = SeatStatus.AVAILABLE;
        private String seatType;
        private String seatId;
        public Builder seatId(String v)            { this.seatId=v; return this;}
        public Builder train(Train v)              { this.train=v; return this; }
        public Builder seatNumber(String v)        { this.seatNumber=v; return this; }
        public Builder coachNumber(String v)       { this.coachNumber=v; return this; }
        public Builder travelClass(TravelClass v)  { this.travelClass=v; return this; }
        public Builder status(SeatStatus v)        { this.status=v; return this; }
        public Builder seatType(String v)          { this.seatType=v; return this; }
        public Seat build() {
            Seat s = new Seat();
            s.seatId=seatId; s.train=train; s.seatNumber=seatNumber; s.coachNumber=coachNumber;
            s.travelClass=travelClass; s.status=status; s.seatType=seatType;
            return s;
        }
    }
}

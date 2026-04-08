package com.railway.booking.model;

/**
 * Passenger — each person travelling on a ticket (Embedded in Ticket document).
 * A ticket can have 1-6 passengers (like IRCTC).
 */
public class Passenger {

    private String passengerId;
    
    private String name;

    private int age;

    private Gender gender;

    private String seatNumber;
    private String coachNumber;

    private Ticket.TicketStatus seatStatus = Ticket.TicketStatus.CONFIRMED;

    private int waitlistNumber;

    public enum Gender { MALE, FEMALE, TRANSGENDER }

    // ── Constructors ──────────────────────────────────────────────
    public Passenger() {}

    // ── Getters / Setters ─────────────────────────────────────────
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getCoachNumber() { return coachNumber; }
    public void setCoachNumber(String coachNumber) { this.coachNumber = coachNumber; }
    public Ticket.TicketStatus getSeatStatus() { return seatStatus; }
    public void setSeatStatus(Ticket.TicketStatus seatStatus) { this.seatStatus = seatStatus; }
    public int getWaitlistNumber() { return waitlistNumber; }
    public void setWaitlistNumber(int waitlistNumber) { this.waitlistNumber = waitlistNumber; }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String passengerId; private String name; private int age;
        private Gender gender; private String seatNumber; private String coachNumber;
        private Ticket.TicketStatus seatStatus = Ticket.TicketStatus.CONFIRMED;
        private int waitlistNumber;
        public Builder passengerId(String v)      { this.passengerId=v; return this; }
        public Builder name(String v)             { this.name=v; return this; }
        public Builder age(int v)                 { this.age=v; return this; }
        public Builder gender(Gender v)           { this.gender=v; return this; }
        public Builder seatNumber(String v)       { this.seatNumber=v; return this; }
        public Builder coachNumber(String v)      { this.coachNumber=v; return this; }
        public Builder seatStatus(Ticket.TicketStatus v) { this.seatStatus=v; return this; }
        public Builder waitlistNumber(int v)      { this.waitlistNumber=v; return this; }
        public Passenger build() {
            Passenger p = new Passenger();
            p.passengerId = passengerId != null ? passengerId : java.util.UUID.randomUUID().toString();
            p.name=name; p.age=age; p.gender=gender;
            p.seatNumber=seatNumber; p.coachNumber=coachNumber;
            p.seatStatus=seatStatus; p.waitlistNumber=waitlistNumber;
            return p;
        }
    }
}

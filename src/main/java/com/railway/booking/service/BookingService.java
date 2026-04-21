package com.railway.booking.service;

import com.railway.booking.dao.*;
import com.railway.booking.dto.BookingRequest;
import com.railway.booking.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE: BookingService — Book, waitlist, PNR (as in MVC diagram)
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final TicketDAO ticketDAO;
    private final TrainDAO trainDAO;
    private final SeatDAO seatDAO;
    private final UserDAO userDAO;
    private final PaymentDAO paymentDAO;
    private final FareService fareService;

    public BookingService(TicketDAO ticketDAO, TrainDAO trainDAO, SeatDAO seatDAO,
                          UserDAO userDAO, PaymentDAO paymentDAO, FareService fareService) {
        this.ticketDAO = ticketDAO;
        this.trainDAO = trainDAO;
        this.seatDAO = seatDAO;
        this.userDAO = userDAO;
        this.paymentDAO = paymentDAO;
        this.fareService = fareService;
    }

    @Transactional
    public Ticket bookTicket(BookingRequest req, String userEmail) {
        User user = userDAO.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Train train = trainDAO.findById(req.getTrainId())
                .orElseThrow(() -> new IllegalArgumentException("Train not found"));

        if (req.getJourneyDate() != null && !train.runsOn(req.getJourneyDate())) {
            throw new IllegalStateException("Train does not run on " + req.getJourneyDate());
        }

        int passengerCount = req.getPassengers() != null ? req.getPassengers().size() : 1;
        double totalFare = fareService.calculateTotalFare(train, req.getTravelClass(),
                passengerCount, false);

        List<Seat> allocatedSeats = allocateSeats(train, req.getTravelClass(), passengerCount);
        Ticket.TicketStatus ticketStatus = allocatedSeats.isEmpty()
                ? Ticket.TicketStatus.WAITLISTED
                : Ticket.TicketStatus.CONFIRMED;

        Ticket ticket = Ticket.builder()
                .user(user)
                .train(train)
                .journeyDate(req.getJourneyDate())
                .boardingStation(req.getBoardingStation())
                .destinationStation(req.getDestinationStation())
                .travelClass(req.getTravelClass())
                .status(ticketStatus)
                .totalFare(totalFare)
                .build();

        List<Passenger> passengers = new ArrayList<>();
        List<BookingRequest.PassengerDto> pdList = req.getPassengers();
        if (pdList != null) {
            for (int i = 0; i < pdList.size(); i++) {
                BookingRequest.PassengerDto pd = pdList.get(i);
                Passenger p = Passenger.builder()
                        .name(pd.getName())
                        .age(pd.getAge())
                        .gender(Passenger.Gender.valueOf(pd.getGender().toUpperCase()))
                        .seatStatus(ticketStatus)
                        .build();

                if (!allocatedSeats.isEmpty() && i < allocatedSeats.size()) {
                    p.setSeatNumber(allocatedSeats.get(i).getSeatNumber());
                    p.setCoachNumber(allocatedSeats.get(i).getCoachNumber());
                } else {
                    p.setWaitlistNumber(getNextWaitlistNumber(train, req.getTravelClass()));
                }
                passengers.add(p);
            }
        }
        ticket.setPassengers(passengers);

        Ticket saved = ticketDAO.save(ticket);
        log.info("Ticket booked: PNR={}, Status={}, User={}", saved.getPnr(), ticketStatus, userEmail);
        return saved;
    }

    @Transactional
    public Payment confirmPayment(String pnr, String paymentMethodStr) {
        Ticket ticket = ticketDAO.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + pnr));

        Payment payment = Payment.builder()
                .ticket(ticket)
                .amount(ticket.getTotalFare())
                .method(Payment.PaymentMethod.valueOf(paymentMethodStr.toUpperCase().replace(" ", "_")))
                .status(Payment.PaymentStatus.SUCCESS)
                .gatewayReference("SIM-" + System.currentTimeMillis())
                .build();

        paymentDAO.save(payment);
        log.info("Payment confirmed: PNR={}, Amount=₹{}", pnr, ticket.getTotalFare());
        return payment;
    }

    @Transactional
    public double cancelTicket(String pnr, String userEmail) {
        Ticket ticket = ticketDAO.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + pnr));

        if (ticket.getUser() == null || ticket.getUser().getEmail() == null) {
            throw new IllegalStateException("Ticket ownership information is missing");
        }

        if (userEmail == null || !ticket.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new SecurityException("You are not authorized to cancel this ticket");
        }
        if (ticket.getStatus() == Ticket.TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already cancelled");
        }

        List<Passenger> passengers = ticket.getPassengers() != null ? ticket.getPassengers() : List.of();
        passengers.forEach(p -> {
            if (p.getSeatNumber() != null && p.getCoachNumber() != null) {
                seatDAO.findByTrainAndTravelClass(ticket.getTrain(), ticket.getTravelClass()).stream()
                        .filter(s -> p.getSeatNumber().equals(s.getSeatNumber())
                                && p.getCoachNumber().equals(s.getCoachNumber()))
                        .findFirst()
                        .ifPresent(s -> seatDAO.updateSeatStatus(s.getSeatId(), Seat.SeatStatus.AVAILABLE));
            }
        });

        double refund = calculateRefund(ticket);
        ticket.setStatus(Ticket.TicketStatus.CANCELLED);
        ticket.setRefundAmount(refund);
        ticket.setCancelledAt(LocalDateTime.now());
        ticketDAO.save(ticket);

        paymentDAO.findByTicket(ticket).ifPresent(p -> {
            p.setStatus(Payment.PaymentStatus.REFUNDED);
            p.setRefundAmount(refund);
            p.setRefundedAt(LocalDateTime.now());
            paymentDAO.save(p);
        });

        promoteWaitlisted(ticket.getTrain(), ticket.getTravelClass());
        log.info("Ticket cancelled: PNR={}, Refund=₹{}", pnr, refund);
        return refund;
    }

    public Ticket getTicketByPnr(String pnr) {
        return ticketDAO.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("No ticket found for PNR: " + pnr));
    }

    public List<Ticket> getUserTickets(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ticketDAO.findByUserOrderByBookedAtDesc(user);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private List<Seat> allocateSeats(Train train, Seat.TravelClass cls, int count) {
        List<Seat> allocated = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            seatDAO.findFirstAvailableSeat(train.getTrainId(), cls.name())
                    .ifPresent(seat -> {
                        seatDAO.updateSeatStatus(seat.getSeatId(), Seat.SeatStatus.BOOKED);
                        allocated.add(seat);
                    });
        }
        return allocated;
    }

    private int getNextWaitlistNumber(Train train, Seat.TravelClass cls) {
        return seatDAO.countWaitlistedSeats(train.getTrainId(), cls) + 1;
    }

    private double calculateRefund(Ticket ticket) {
        long hoursBeforeJourney = java.time.Duration.between(
                LocalDateTime.now(),
                ticket.getJourneyDate().atTime(ticket.getTrain().getDepartureTime())
        ).toHours();

        double fare = ticket.getTotalFare();
        if (hoursBeforeJourney > 48)  return fare - 60;
        if (hoursBeforeJourney > 24)  return fare * 0.75;
        if (hoursBeforeJourney > 12)  return fare * 0.50;
        if (hoursBeforeJourney > 4)   return fare * 0.25;
        return 0;
    }

    private void promoteWaitlisted(Train train, Seat.TravelClass cls) {
        List<Ticket> waitlisted = ticketDAO.findByStatus(Ticket.TicketStatus.WAITLISTED);
        waitlisted.stream()
                .filter(t -> t.getTrain().getTrainId().equals(train.getTrainId())
                        && t.getTravelClass().equals(cls))
                .findFirst()
                .ifPresent(t -> {
                    List<Seat> seats = allocateSeats(train, cls, t.getPassengers().size());
                    if (seats.size() == t.getPassengers().size()) {
                        t.setStatus(Ticket.TicketStatus.CONFIRMED);
                        for (int i = 0; i < t.getPassengers().size(); i++) {
                            t.getPassengers().get(i).setSeatNumber(seats.get(i).getSeatNumber());
                            t.getPassengers().get(i).setCoachNumber(seats.get(i).getCoachNumber());
                            t.getPassengers().get(i).setSeatStatus(Ticket.TicketStatus.CONFIRMED);
                        }
                        ticketDAO.save(t);
                        log.info("Waitlisted ticket promoted to CONFIRMED: PNR={}", t.getPnr());
                    } else {
                        // Revert seat allocation if we couldn't fulfill the entire ticket
                        for (Seat s : seats) {
                            seatDAO.updateSeatStatus(s.getSeatId(), Seat.SeatStatus.AVAILABLE);
                        }
                    }
                });
    }
}

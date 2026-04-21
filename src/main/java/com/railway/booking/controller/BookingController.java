package com.railway.booking.controller;

import com.railway.booking.dto.BookingRequest;
import com.railway.booking.model.*;
import com.railway.booking.service.BookingService;
import com.railway.booking.service.PaymentService;
import com.railway.booking.service.TrainService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * CONTROLLER: BookingController — Booking, cancellation, PNR (as in MVC diagram)
 * Handles BookingView, TicketView, Cancel Dialog, PaymentView.
 */
@Controller
@RequestMapping("/booking")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final TrainService   trainService;

    public BookingController(BookingService bookingService, PaymentService paymentService, TrainService trainService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.trainService = trainService;
    }

    // ── Booking Form ──────────────────────────────────────────────────

    @GetMapping("/new")
    public String bookingForm(@RequestParam String trainId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam String travelClass,
                              @RequestParam(defaultValue = "1") int passengers,
                              Model model) {
        Train train = trainService.getTrainById(trainId);
        BookingRequest req = new BookingRequest();
        req.setTrainId(trainId);
        req.setJourneyDate(date);
        req.setTravelClass(Seat.TravelClass.valueOf(travelClass));
        req.setBoardingStation(train.getSource());
        req.setDestinationStation(train.getDestination());

        model.addAttribute("train", train);
        model.addAttribute("bookingRequest", req);
        model.addAttribute("passengerCount", passengers);
        model.addAttribute("travelClass", Seat.TravelClass.valueOf(travelClass));
        model.addAttribute("paymentMethods", Payment.PaymentMethod.values());
        model.addAttribute("genderOptions", Passenger.Gender.values());
        return "booking";
    }

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @PostMapping("/confirm")
    public String confirmBooking(@Valid @ModelAttribute("bookingRequest") BookingRequest req,
                                 BindingResult result,
                                 @AuthenticationPrincipal User user,
                                 Model model,
                                 RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            Train train = trainService.getTrainById(req.getTrainId());
            model.addAttribute("train", train);
            model.addAttribute("paymentMethods", Payment.PaymentMethod.values());
            model.addAttribute("genderOptions", Passenger.Gender.values());
            return "booking";
        }
        try {
            Ticket ticket = bookingService.bookTicket(req, user.getEmail());
            log.info("Ticket booked successfully: PNR={}, redirecting to payment selection", ticket.getPnr());
            return "redirect:/booking/payment/" + ticket.getPnr();
        } catch (Exception e) {
            log.error("Booking failed: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Booking failed: " + e.getMessage());
            return "redirect:/booking/new?trainId=" + req.getTrainId()
                    + "&date=" + req.getJourneyDate()
                    + "&travelClass=" + req.getTravelClass()
                    + "&passengers=" + req.getPassengers().size();
        }
    }

    @PostMapping("/razorpay-success")
    public String razorpaySuccess(@RequestParam String pnr,
                                  @RequestParam String razorpay_payment_id,
                                  @RequestParam String razorpay_order_id,
                                  @RequestParam String razorpay_signature,
                                  @AuthenticationPrincipal User user,
                                  RedirectAttributes redirectAttrs) {
        log.info("Razorpay callback received: OrderId={} for PNR={}", razorpay_order_id, pnr);
        Ticket ticket = bookingService.getTicketByPnr(pnr);
        assertTicketAccessibleByUser(ticket, user);

        boolean signatureValid = paymentService.verifyRazorpaySignature(
                razorpay_order_id,
                razorpay_payment_id,
                razorpay_signature
        );
        if (!signatureValid) {
            log.warn("Rejected Razorpay callback due to invalid signature for PNR={}", pnr);
            redirectAttrs.addFlashAttribute("error", "Payment verification failed. Please contact support if amount was debited.");
            return "redirect:/booking/payment/" + pnr;
        }

        bookingService.confirmPayment(pnr, "RAZORPAY");
        redirectAttrs.addFlashAttribute("success", "Razorpay Payment Successful! PNR: " + pnr);
        return "redirect:/booking/ticket/" + pnr;
    }

    // ── Ticket / PNR ─────────────────────────────────────────────────

    @GetMapping("/ticket/{pnr}")
    public String ticketDetail(@PathVariable String pnr,
                               @AuthenticationPrincipal User user,
                               Model model) {
        Ticket ticket = bookingService.getTicketByPnr(pnr);
        assertTicketAccessibleByUser(ticket, user);
        Payment payment = paymentService.getPaymentByTicket(ticket);
        model.addAttribute("ticket", ticket);
        model.addAttribute("payment", payment);
        return "ticket";
    }

    /** Authenticated PNR status lookup */
    @GetMapping("/pnr")
    public String pnrLookup(@AuthenticationPrincipal User user,
                            @RequestParam(required = false) String pnr,
                            Model model) {
        if (pnr != null && !pnr.isBlank()) {
            String sanitizedPnr = pnr.trim();
            if (!sanitizedPnr.matches("^[0-9]{10}$")) {
                model.addAttribute("error", "PNR must be exactly 10 digits.");
                model.addAttribute("pnr", sanitizedPnr);
                return "pnr-status";
            }
            try {
                Ticket ticket = bookingService.getTicketByPnr(sanitizedPnr);
                assertTicketAccessibleByUser(ticket, user);
                Payment payment = paymentService.getPaymentByTicket(ticket);
                model.addAttribute("ticket", ticket);
                model.addAttribute("payment", payment);
            } catch (AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                model.addAttribute("error", "No booking found for this account and PNR.");
            }
        }
        model.addAttribute("pnr", pnr);
        return "pnr-status";
    }

    // ── My Bookings ───────────────────────────────────────────────────

    @GetMapping("/my-bookings")
    public String myBookings(@AuthenticationPrincipal User user, Model model) {
        List<Ticket> tickets = bookingService.getUserTickets(user.getEmail());
        model.addAttribute("tickets", tickets);
        return "my-bookings";
    }

    // ── Cancellation ──────────────────────────────────────────────────

    @GetMapping("/cancel/{pnr}")
    public String cancelPage(@PathVariable String pnr,
                             @AuthenticationPrincipal User user,
                             Model model) {
        Ticket ticket = bookingService.getTicketByPnr(pnr);
        assertTicketAccessibleByUser(ticket, user);
        model.addAttribute("ticket", ticket);
        model.addAttribute("routeSummary", formatRouteSummary(ticket));
        model.addAttribute("travelClassLabel", ticket.getTravelClass() != null ? ticket.getTravelClass().getDisplayName() : "Unknown");
        model.addAttribute("journeyDateLabel", ticket.getJourneyDate() != null ? ticket.getJourneyDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "Unknown");
        model.addAttribute("fareLabel", String.format("Rs %,d", Math.round(ticket.getTotalFare())));
        return "cancel";
    }

    @PostMapping("/cancel/{pnr}")
    public String cancelTicket(@PathVariable String pnr,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttrs) {
        try {
            double refund = bookingService.cancelTicket(pnr, user.getEmail());
            redirectAttrs.addFlashAttribute("success",
                    String.format("Ticket cancelled. Refund of ₹%.0f will be credited in 5-7 days.", refund));
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/booking/my-bookings";
    }

    // ── Payment (simulated) ───────────────────────────────────────────

    @GetMapping("/payment/{pnr}")
    public String paymentPage(@PathVariable String pnr,
                              @AuthenticationPrincipal User user,
                              Model model) {
        Ticket ticket = bookingService.getTicketByPnr(pnr);
        assertTicketAccessibleByUser(ticket, user);
        model.addAttribute("ticket", ticket);
        model.addAttribute("paymentMethods", Payment.PaymentMethod.values());
        return "payment";
    }

    @PostMapping("/initiate-payment/{pnr}")
    public String initiatePayment(@PathVariable String pnr,
                                  @RequestParam String paymentMethod,
                                  @AuthenticationPrincipal User user,
                                  Model model,
                                  RedirectAttributes redirectAttrs) {
        try {
            Ticket ticket = bookingService.getTicketByPnr(pnr);
            assertTicketAccessibleByUser(ticket, user);

            if ("RAZORPAY".equalsIgnoreCase(paymentMethod)) {
                // Generate Razorpay Order for real payment gateway
                java.util.Map<String, Object> rzrOrder = paymentService.initiatePayment(pnr, "RAZORPAY", ticket.getTotalFare());
                model.addAttribute("ticket", ticket);
                model.addAttribute("razorpayOrder", rzrOrder);
                model.addAttribute("razorpayKeyId", razorpayKeyId);
                model.addAttribute("user", user);
                log.info("Razorpay payment initiated for PNR={}, OrderId={}", pnr, rzrOrder.get("orderId"));
                return "razorpay_checkout";
            } else {
                // For other payment methods, directly confirm payment (simulated)
                bookingService.confirmPayment(pnr, paymentMethod);
                redirectAttrs.addFlashAttribute("success", "Payment successful! PNR: " + pnr);
                log.info("Payment confirmed for PNR={}, Method={}", pnr, paymentMethod);
                return "redirect:/booking/ticket/" + pnr;
            }
        } catch (Exception e) {
            log.error("Payment initiation failed for PNR={}: {}", pnr, e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Payment initiation failed: " + e.getMessage());
            return "redirect:/booking/payment/" + pnr;
        }
    }

    private void assertTicketAccessibleByUser(Ticket ticket, User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        boolean owner = ticket.getUser() != null && user.getEmail().equalsIgnoreCase(ticket.getUser().getEmail());
        boolean admin = user.getRole() == User.Role.ADMIN;
        if (!owner && !admin) {
            throw new AccessDeniedException("You can access only your own ticket details");
        }
    }

    private String formatRouteSummary(Ticket ticket) {
        String boardingStation = ticket.getBoardingStation() != null ? ticket.getBoardingStation() : "Unknown";
        String destinationStation = ticket.getDestinationStation() != null ? ticket.getDestinationStation() : "Unknown";
        return boardingStation + " -> " + destinationStation;
    }
}

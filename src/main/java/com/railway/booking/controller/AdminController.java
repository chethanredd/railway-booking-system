package com.railway.booking.controller;

import com.railway.booking.dao.UserDAO;
import com.railway.booking.model.Train;
import com.railway.booking.service.BookingService;
import com.railway.booking.service.PaymentService;
import com.railway.booking.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CONTROLLER: AdminController — Add, update, delete (as in MVC diagram)
 * Secured to ADMIN role only.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final TrainService   trainService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final UserDAO        userDAO;

    public AdminController(TrainService trainService, BookingService bookingService, PaymentService paymentService, UserDAO userDAO) {
        this.trainService = trainService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.userDAO = userDAO;
    }

    /** Admin dashboard panel */
    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(Model model) {
        model.addAttribute("totalTrains",   trainService.getAllTrains().size());
        model.addAttribute("totalUsers",    userDAO.count());
        model.addAttribute("totalBookings", paymentService.getTotalBookings());
        model.addAttribute("totalRevenue",  paymentService.getTotalRevenue());
        model.addAttribute("recentBookings", bookingService.getUserTickets(""));
        return "admin/dashboard";
    }

    // ── Train Management ──────────────────────────────────────────────

    @GetMapping("/trains")
    public String manageTrains(Model model) {
        model.addAttribute("trains", trainService.getAllTrains());
        model.addAttribute("trainTypes", Train.TrainType.values());
        return "admin/trains";
    }

    @GetMapping("/trains/add")
    public String addTrainForm(Model model) {
        model.addAttribute("train", new Train());
        model.addAttribute("trainTypes", Train.TrainType.values());
        return "admin/train-form";
    }

    @PostMapping("/trains/add")
    public String addTrain(@ModelAttribute Train train, RedirectAttributes redirectAttrs) {
        try {
            trainService.addTrain(train);
            redirectAttrs.addFlashAttribute("success", "Train added: " + train.getTrainName());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/trains";
    }

    @GetMapping("/trains/edit/{id}")
    public String editTrainForm(@PathVariable String id, Model model) {
        Train train = trainService.getTrainById(id);
        model.addAttribute("train", train);
        model.addAttribute("trainTypes", Train.TrainType.values());
        return "admin/train-form";
    }

    @PostMapping("/trains/edit/{id}")
    public String updateTrain(@PathVariable String id,
                              @ModelAttribute Train train,
                              RedirectAttributes redirectAttrs) {
        train.setTrainId(id);
        try {
            trainService.updateTrain(train);
            redirectAttrs.addFlashAttribute("success", "Train updated successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/trains";
    }

    @PostMapping("/trains/delete/{id}")
    public String deleteTrain(@PathVariable String id, RedirectAttributes redirectAttrs) {
        try {
            trainService.deleteTrain(id);
            redirectAttrs.addFlashAttribute("success", "Train deleted.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Cannot delete: " + e.getMessage());
        }
        return "redirect:/admin/trains";
    }

    // ── User Management ───────────────────────────────────────────────

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userDAO.findAll());
        return "admin/users";
    }

    // ── Booking Management ────────────────────────────────────────────

    @GetMapping("/bookings")
    public String manageBookings(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "admin/bookings";
    }
}

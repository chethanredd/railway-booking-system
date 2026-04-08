package com.railway.booking.controller;

import com.railway.booking.dto.RegisterRequest;
import com.railway.booking.exception.DuplicateEmailException;
import com.railway.booking.model.User;
import com.railway.booking.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CONTROLLER: AuthController — Login, register (as in MVC diagram)
 * Handles LoginView and RegisterView.
 */
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Login ────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid email or password. Please try again.");
        if (logout != null) model.addAttribute("message", "You have been logged out successfully.");
        return "login";
    }

    // ── Register ─────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest req,
                           BindingResult result,
                           RedirectAttributes redirectAttrs,
                           Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        if (!req.isPasswordMatch()) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }
        try {
            authService.register(req);
            redirectAttrs.addFlashAttribute("success",
                    "Account created! Please log in.");
            return "redirect:/login";
        } catch (DuplicateEmailException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    // ── Profile ───────────────────────────────────────────────────────

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal User user,
                                @RequestParam String name,
                                @RequestParam String mobile,
                                RedirectAttributes redirectAttrs) {
        authService.updateProfile(user.getUserId(), name, mobile);
        redirectAttrs.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal User user,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttrs) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/profile";
        }
        try {
            authService.changePassword(user.getEmail(), oldPassword, newPassword);
            redirectAttrs.addFlashAttribute("success", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}

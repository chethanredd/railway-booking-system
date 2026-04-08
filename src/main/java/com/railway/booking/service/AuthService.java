package com.railway.booking.service;

import com.railway.booking.dao.UserDAO;
import com.railway.booking.dto.RegisterRequest;
import com.railway.booking.exception.DuplicateEmailException;
import com.railway.booking.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SERVICE: AuthService — Validate, sessions (as in MVC diagram)
 */
@Service
public class AuthService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userDAO.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for: " + email));
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userDAO.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + req.getEmail());
        }
        if (req.getMobile() != null && userDAO.existsByMobile(req.getMobile())) {
            throw new IllegalArgumentException("Mobile number already registered");
        }
        if (!req.isPasswordMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .mobile(req.getMobile())
                .role(User.Role.PASSENGER)
                .build();

        User saved = userDAO.save(user);
        log.info("New user registered: {} ({})", saved.getName(), saved.getEmail());
        return saved;
    }

    public User findByEmail(String email) {
        return userDAO.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public void updateProfile(String userId, String name, String mobile) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setName(name);
        user.setMobile(mobile);
        userDAO.save(user);
        log.info("Profile updated for user id={}", userId);
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = (User) loadUserByUsername(email);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userDAO.save(user);
        log.info("Password changed for user: {}", email);
    }
}

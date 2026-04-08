package com.railway.booking.model;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * MODEL: User — userId, name, email (as in MVC diagram)
 * Implements UserDetails for Spring Security integration.
 */
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String userId;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String mobile;

    private Role role = Role.PASSENGER;

    private boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { PASSENGER, ADMIN }

    // ── Constructors ──────────────────────────────────────────────
    public User() {}

    public User(String userId, String name, String email, String password, String mobile,
                Role role, boolean enabled, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobile = mobile;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public String getUserId()  { return userId; }
    public void setUserId(String userId)  { this.userId = userId; }
    public String getName()  { return name; }
    public void setName(String name)  { this.name = name; }
    public String getEmail()  { return email; }
    public void setEmail(String email)  { this.email = email; }
    public void setPassword(String password)  { this.password = password; }
    public String getMobile()  { return mobile; }
    public void setMobile(String mobile)  { this.mobile = mobile; }
    public Role getRole()  { return role; }
    public void setRole(Role role)  { this.role = role; }
    public void setEnabled(boolean enabled)  { this.enabled = enabled; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)  { this.createdAt = createdAt; }

    // ── Spring Security ───────────────────────────────────────────
    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return email; }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String userId; private String name; private String email;
        private String password; private String mobile;
        private Role role = Role.PASSENGER; private boolean enabled = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        public Builder userId(String v)  { this.userId=v; return this; }
        public Builder name(String v)    { this.name=v; return this; }
        public Builder email(String v)   { this.email=v; return this; }
        public Builder password(String v){ this.password=v; return this; }
        public Builder mobile(String v)  { this.mobile=v; return this; }
        public Builder role(Role v)      { this.role=v; return this; }
        public Builder enabled(boolean v){ this.enabled=v; return this; }
        public User build() {
            User u = new User();
            u.userId=userId; u.name=name; u.email=email; u.password=password;
            u.mobile=mobile; u.role=role; u.enabled=enabled; u.createdAt=createdAt;
            return u;
        }
    }
}

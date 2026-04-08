package com.railway.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RailWay Pro - Next-Gen Railway Ticket Booking System
 * Inspired by IRCTC but with simplified UX and real-time seat management.
 *
 * MVC Architecture:
 *  View     → Thymeleaf templates (src/main/resources/templates/)
 *  Controller → com.railway.booking.controller.*
 *  Service  → com.railway.booking.service.*
 *  Model    → com.railway.booking.model.*
 *  DAO      → com.railway.booking.dao.*  (Spring Data Mongo Repositories)
 *  DB       → MongoDB Cloud
 */
@SpringBootApplication
@EnableScheduling
public class RailwayBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RailwayBookingApplication.class, args);
    }
}

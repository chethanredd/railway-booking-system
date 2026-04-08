package com.railway.booking.config;

import com.railway.booking.dao.SeatDAO;
import com.railway.booking.dao.TrainDAO;
import com.railway.booking.dao.UserDAO;
import com.railway.booking.model.*;
import com.mongodb.MongoTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * DataSeeder — populates H2 in-memory database with realistic demo data.
 * Creates admin user, 10 trains, and seats for each.
 * This runs automatically on startup in dev (H2) mode.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserDAO        userDAO;
    private final TrainDAO       trainDAO;
    private final SeatDAO        seatDAO;
    private final PasswordEncoder encoder;

    @Value("${app.dataset.import.enabled:false}")
    private boolean datasetImportEnabled;

    public DataSeeder(UserDAO userDAO, TrainDAO trainDAO, SeatDAO seatDAO, PasswordEncoder encoder) {
        this.userDAO = userDAO;
        this.trainDAO = trainDAO;
        this.seatDAO = seatDAO;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        try {
            seedUsers();
            seedTrains();
            log.info("✅ Demo data seeded. Visit http://localhost:8080");
            log.info("   Admin: admin@railway.in / admin123");
            log.info("   User:  user@railway.in  / user1234");
            log.info("   H2 Console: http://localhost:8080/h2-console");
        } catch (DataAccessException | MongoTimeoutException ex) {
            log.warn("Skipping DataSeeder because MongoDB is unavailable: {}", ex.getMessage());
        }
    }

    private void seedUsers() {
        if (userDAO.count() > 0) return;

        userDAO.saveAll(List.of(
            User.builder().name("System Admin").email("admin@railway.in")
                .password(encoder.encode("admin123")).mobile("9999999999")
                .role(User.Role.ADMIN).build(),

            User.builder().name("Rahul Sharma").email("user@railway.in")
                .password(encoder.encode("user1234")).mobile("9876543210")
                .role(User.Role.PASSENGER).build(),

            User.builder().name("Priya Singh").email("priya@example.com")
                .password(encoder.encode("user1234")).mobile("9123456780")
                .role(User.Role.PASSENGER).build()
        ));
        log.info("Seeded {} users", userDAO.count());
    }

    private void seedTrains() {
        if (datasetImportEnabled) {
            log.info("Dataset import is enabled; skipping built-in demo train seed.");
            return;
        }

        List<Train> trains = List.of(
            buildTrain("12301", "Howrah Rajdhani Express",    "New Delhi",      "Howrah Junction",   LocalTime.of(16,55), LocalTime.of(9,55),  "MON,TUE,WED,THU,FRI,SAT,SUN", 1080, 1453, Train.TrainType.RAJDHANI),
            buildTrain("12951", "Mumbai Rajdhani Express",    "New Delhi",      "Mumbai Central",    LocalTime.of(16,25), LocalTime.of(8,35),  "MON,TUE,WED,THU,FRI,SAT,SUN", 955,  1384, Train.TrainType.RAJDHANI),
            buildTrain("12002", "New Delhi Shatabdi Express", "New Delhi",      "Bhopal Junction",  LocalTime.of(6,0),   LocalTime.of(13,40), "MON,TUE,WED,THU,FRI,SAT,SUN", 460,  701,  Train.TrainType.SHATABDI),
            buildTrain("12622", "Tamil Nadu Express",         "New Delhi",      "Chennai Central",   LocalTime.of(22,30), LocalTime.of(7,10),  "MON,TUE,WED,THU,FRI,SAT,SUN", 2080, 2186, Train.TrainType.MAIL_EXPRESS),
            buildTrain("12259", "Duronto Express",            "New Delhi",      "Sealdah",           LocalTime.of(8,10),  LocalTime.of(4,30),  "MON,WED,FRI,SAT,SUN",          1220, 1528, Train.TrainType.DURONTO),
            buildTrain("12009", "Mumbai Shatabdi",            "Mumbai Central", "Ahmedabad Junction",LocalTime.of(6,25),  LocalTime.of(12,55), "MON,TUE,WED,THU,FRI,SAT,SUN", 390,  491,  Train.TrainType.SHATABDI),
            buildTrain("12163", "Chennai Dadar Express",      "Chennai Central","Mumbai Central",    LocalTime.of(18,40), LocalTime.of(20,10), "MON,TUE,WED,THU,FRI,SAT,SUN", 1500, 1279, Train.TrainType.SUPERFAST_EXPRESS),
            buildTrain("12723", "Telangana Express",          "Hyderabad",      "New Delhi",         LocalTime.of(6,30),  LocalTime.of(11,55), "MON,TUE,WED,THU,FRI,SAT,SUN", 2940, 1661, Train.TrainType.SUPERFAST_EXPRESS),
            buildTrain("22691", "Rajdhani Express",           "Bangalore City", "New Delhi",         LocalTime.of(20,0),  LocalTime.of(5,30),  "MON,TUE,WED,THU,FRI,SAT,SUN", 2180, 2444, Train.TrainType.RAJDHANI),
            buildTrain("22439", "Vande Bharat Express",       "New Delhi",      "Varanasi",          LocalTime.of(6,0),   LocalTime.of(14,0),  "MON,TUE,WED,THU,FRI,SAT,SUN", 480,  820,  Train.TrainType.VANDE_BHARAT),
            buildTrain("12841", "Coromandel Express",         "Howrah Junction", "Chennai Central",   LocalTime.of(14,15), LocalTime.of(16,40), "MON,TUE,WED,THU,FRI,SAT,SUN", 1585, 1662, Train.TrainType.SUPERFAST_EXPRESS),
            buildTrain("12628", "Karnataka Express",          "New Delhi",      "Bangalore City",    LocalTime.of(20,20), LocalTime.of(6,40),  "MON,TUE,WED,THU,FRI,SAT,SUN", 2080, 2404, Train.TrainType.SUPERFAST_EXPRESS),
            buildTrain("12295", "Sanghamitra Express",        "Bangalore City", "Patna Junction",    LocalTime.of(9,30),  LocalTime.of(10,20), "MON,TUE,WED,THU,FRI,SAT,SUN", 1490, 2678, Train.TrainType.SUPERFAST_EXPRESS),
            buildTrain("12310", "Rajendra Nagar Tejas",       "New Delhi",      "Patna Junction",    LocalTime.of(17,10), LocalTime.of(6,45),  "MON,TUE,WED,THU,FRI,SAT,SUN", 815,  1001, Train.TrainType.SUPERFAST_EXPRESS),
            buildTrain("12904", "Golden Temple Mail",         "Amritsar",       "Mumbai Central",    LocalTime.of(21,25), LocalTime.of(5,45),  "MON,TUE,WED,THU,FRI,SAT,SUN", 1940, 1891, Train.TrainType.MAIL_EXPRESS),
            buildTrain("12618", "Mangala Lakshadweep Express", "Nizamuddin",     "Ernakulam Junction", LocalTime.of(5,40),  LocalTime.of(8,35),  "MON,TUE,WED,THU,FRI,SAT,SUN", 3055, 2680, Train.TrainType.MAIL_EXPRESS)
        );

        int inserted = 0;
        for (Train train : trains) {
            if (trainDAO.existsByTrainNumber(train.getTrainNumber())) {
                continue;
            }
            Train saved = trainDAO.save(train);
            seedSeatsForTrain(saved);
            inserted++;
        }
        log.info("Inserted {} missing trains from broader dataset. Total trains now: {}", inserted, trainDAO.count());
    }

    private Train buildTrain(String num, String name, String src, String dst,
                             LocalTime dep, LocalTime arr, String days,
                             int durationMin, double distKm, Train.TrainType type) {
        return Train.builder()
                .trainNumber(num).trainName(name)
                .source(src).destination(dst)
                .departureTime(dep).arrivalTime(arr)
                .runningDays(days)
                .durationMinutes(durationMin)
                .distanceKm(distKm)
                .trainType(type)
                .build();
    }

    /** Creates realistic coach/seat layout for each train */
    private void seedSeatsForTrain(Train train) {
        // Sleeper coaches: S1–S8, 72 seats each
        for (int coach = 1; coach <= 8; coach++) {
            for (int seat = 1; seat <= 72; seat++) {
                seatDAO.save(Seat.builder()
                    .train(train)
                    .coachNumber("S" + coach)
                    .seatNumber(String.valueOf(seat))
                    .travelClass(Seat.TravelClass.SLEEPER)
                    .seatType(getSeatType(seat))
                    .status(Seat.SeatStatus.AVAILABLE)
                    .build());
            }
        }
        // AC 3-Tier: B1–B4, 64 seats each
        for (int coach = 1; coach <= 4; coach++) {
            for (int seat = 1; seat <= 64; seat++) {
                seatDAO.save(Seat.builder()
                    .train(train)
                    .coachNumber("B" + coach)
                    .seatNumber(String.valueOf(seat))
                    .travelClass(Seat.TravelClass.AC_3_TIER)
                    .seatType(getSeatType(seat))
                    .status(Seat.SeatStatus.AVAILABLE)
                    .build());
            }
        }
        // AC 2-Tier: A1–A2, 48 seats each
        for (int coach = 1; coach <= 2; coach++) {
            for (int seat = 1; seat <= 48; seat++) {
                seatDAO.save(Seat.builder()
                    .train(train)
                    .coachNumber("A" + coach)
                    .seatNumber(String.valueOf(seat))
                    .travelClass(Seat.TravelClass.AC_2_TIER)
                    .seatType(seat % 2 == 0 ? "UPPER" : "LOWER")
                    .status(Seat.SeatStatus.AVAILABLE)
                    .build());
            }
        }
        // Chair Car: C1–C3, 78 seats each
        for (int coach = 1; coach <= 3; coach++) {
            for (int seat = 1; seat <= 78; seat++) {
                seatDAO.save(Seat.builder()
                    .train(train)
                    .coachNumber("CC" + coach)
                    .seatNumber(String.valueOf(seat))
                    .travelClass(Seat.TravelClass.CC)
                    .seatType(seat % 3 == 0 ? "WINDOW" : "AISLE")
                    .status(Seat.SeatStatus.AVAILABLE)
                    .build());
            }
        }
    }

    private String getSeatType(int seatNum) {
        int pos = ((seatNum - 1) % 8) + 1;
        return switch (pos) {
            case 1, 4 -> "LOWER";
            case 2, 5 -> "MIDDLE";
            case 3, 6 -> "UPPER";
            case 7    -> "SIDE_LOWER";
            case 8    -> "SIDE_UPPER";
            default   -> "LOWER";
        };
    }
}

package com.railway.booking.config;

import com.railway.booking.dao.SeatDAO;
import com.railway.booking.dao.TrainDAO;
import com.railway.booking.model.Seat;
import com.railway.booking.model.Train;
import com.mongodb.MongoTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TrainDAO trainDAO;
    private final SeatDAO seatDAO;

    public DataInitializer(TrainDAO trainDAO, SeatDAO seatDAO) {
        this.trainDAO = trainDAO;
        this.seatDAO = seatDAO;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            if (trainDAO.count() == 0) {
                log.info("Database empty! Bootstrapping realistic IRCTC Train Data...");
                seedTrains();
                log.info("Train Data seeded successfully.");
            } else {
                log.info("Trains already exist in DB. Skipping bootstrap.");
            }
        } catch (DataAccessException | MongoTimeoutException ex) {
            log.warn("Skipping DataInitializer because MongoDB is unavailable: {}", ex.getMessage());
        }
    }

    private void seedTrains() {
        // 1. New Delhi - Bhopal Shatabdi Express (12002)
        Train shatabdi = Train.builder()
                .trainNumber("12002")
                .trainName("Bhopal Shatabdi Exp")
                .source("New Delhi (NDLS)")
                .destination("Rani Kamlapati (RKMP)")
                .departureTime(LocalTime.of(6, 0))
                .arrivalTime(LocalTime.of(14, 40))
                .durationMinutes(520)
                .distanceKm(702)
                .runningDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .trainType(Train.TrainType.SHATABDI)
                .active(true)
                .routeStops(Arrays.asList(
                        new Train.RouteStop("New Delhi (NDLS)", LocalTime.of(6, 0), LocalTime.of(6, 0), 1, 0),
                        new Train.RouteStop("Mathura Jn (MTJ)", LocalTime.of(7, 19), LocalTime.of(7, 20), 1, 141),
                        new Train.RouteStop("Agra Cantt (AGC)", LocalTime.of(7, 50), LocalTime.of(7, 55), 1, 195),
                        new Train.RouteStop("Gwalior Jn (GWL)", LocalTime.of(9, 23), LocalTime.of(9, 28), 1, 313),
                        new Train.RouteStop("VGL Jhansi Jn (VGLJ)", LocalTime.of(10, 45), LocalTime.of(10, 50), 1, 410),
                        new Train.RouteStop("Rani Kamlapati (RKMP)", LocalTime.of(14, 40), LocalTime.of(14, 40), 1, 702)
                ))
                .build();
        trainDAO.save(shatabdi);
        generateSeatsForTrain(shatabdi, 2, 8); // 2 Exec Chair Car, 8 Chair Car

        // 2. Mumbai CSMT - Howrah Jn Gitanjali Express (12859)
        Train gitanjali = Train.builder()
                .trainNumber("12859")
                .trainName("Gitanjali Express")
                .source("Mumbai CSMT")
                .destination("Howrah Jn (HWH)")
                .departureTime(LocalTime.of(6, 0))
                .arrivalTime(LocalTime.of(12, 30))
                .durationMinutes(1830) // Over a day
                .distanceKm(1968)
                .runningDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .trainType(Train.TrainType.SUPERFAST_EXPRESS)
                .active(true)
                .routeStops(Arrays.asList(
                        new Train.RouteStop("Mumbai CSMT", LocalTime.of(6, 0), LocalTime.of(6, 0), 1, 0),
                        new Train.RouteStop("Dadar (DR)", LocalTime.of(6, 12), LocalTime.of(6, 15), 1, 9),
                        new Train.RouteStop("Kalyan Jn (KYN)", LocalTime.of(6, 52), LocalTime.of(6, 55), 1, 51),
                        new Train.RouteStop("Bhusaval Jn (BSL)", LocalTime.of(12, 0), LocalTime.of(12, 5), 1, 440),
                        new Train.RouteStop("Nagpur Jn (NGP)", LocalTime.of(18, 55), LocalTime.of(19, 0), 1, 833),
                        new Train.RouteStop("Raipur Jn (R)", LocalTime.of(23, 50), LocalTime.of(23, 55), 1, 1134),
                        new Train.RouteStop("Tatanagar Jn (TATA)", LocalTime.of(8, 20), LocalTime.of(8, 25), 2, 1718),
                        new Train.RouteStop("Howrah Jn (HWH)", LocalTime.of(12, 30), LocalTime.of(12, 30), 2, 1968)
                ))
                .build();
        trainDAO.save(gitanjali);
        generateSeatsForSleeperTrain(gitanjali, 12, 4, 2, 1); // 12 SL, 4 3A, 2 2A, 1 1A

        // 3. New Delhi - Varanasi Vande Bharat (22436)
        Train vande = Train.builder()
                .trainNumber("22436")
                .trainName("NDLS BSB Vande Bharat Exp")
                .source("New Delhi (NDLS)")
                .destination("Varanasi Jn (BSB)")
                .departureTime(LocalTime.of(6, 0))
                .arrivalTime(LocalTime.of(14, 0))
                .durationMinutes(480)
                .distanceKm(759)
                .runningDays("MON,TUE,WED,FRI,SAT,SUN") // Doesn't run on Thursday
                .trainType(Train.TrainType.VANDE_BHARAT)
                .active(true)
                .routeStops(Arrays.asList(
                        new Train.RouteStop("New Delhi (NDLS)", LocalTime.of(6, 0), LocalTime.of(6, 0), 1, 0),
                        new Train.RouteStop("Kanpur Central (CNB)", LocalTime.of(10, 8), LocalTime.of(10, 10), 1, 440),
                        new Train.RouteStop("Prayagraj Jn (PRYJ)", LocalTime.of(12, 8), LocalTime.of(12, 10), 1, 634),
                        new Train.RouteStop("Varanasi Jn (BSB)", LocalTime.of(14, 0), LocalTime.of(14, 0), 1, 759)
                ))
                .build();
        trainDAO.save(vande);
        generateSeatsForTrain(vande, 2, 14); // 2 Exec Chair Car, 14 Chair Car

        // 4. Chennai Central - New Delhi Grand Trunk Express (12615)
        Train gtEx = Train.builder()
                .trainNumber("12615")
                .trainName("Grand Trunk Express")
                .source("MGR Chennai Central (MAS)")
                .destination("New Delhi (NDLS)")
                .departureTime(LocalTime.of(18, 50))
                .arrivalTime(LocalTime.of(5, 5))
                .durationMinutes(2055)
                .distanceKm(2181)
                .runningDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .trainType(Train.TrainType.SUPERFAST_EXPRESS)
                .active(true)
                .routeStops(Arrays.asList(
                        new Train.RouteStop("MGR Chennai Central (MAS)", LocalTime.of(18, 50), LocalTime.of(18, 50), 1, 0),
                        new Train.RouteStop("Vijayawada Jn (BZA)", LocalTime.of(1, 5), LocalTime.of(1, 15), 2, 431),
                        new Train.RouteStop("Balharshah Jn (BPQ)", LocalTime.of(7, 45), LocalTime.of(7, 50), 2, 881),
                        new Train.RouteStop("Nagpur Jn (NGP)", LocalTime.of(11, 30), LocalTime.of(11, 35), 2, 1089),
                        new Train.RouteStop("Bhopal Jn (BPL)", LocalTime.of(18, 15), LocalTime.of(18, 20), 2, 1478),
                        new Train.RouteStop("VGL Jhansi Jn (VGLJ)", LocalTime.of(22, 40), LocalTime.of(22, 48), 2, 1769),
                        new Train.RouteStop("Mathura Jn (MTJ)", LocalTime.of(2, 40), LocalTime.of(2, 45), 3, 2038),
                        new Train.RouteStop("New Delhi (NDLS)", LocalTime.of(5, 5), LocalTime.of(5, 5), 3, 2181)
                ))
                .build();
        trainDAO.save(gtEx);
        generateSeatsForSleeperTrain(gtEx, 10, 5, 2, 1);
    }

    private void generateSeatsForTrain(Train train, int ecCoaches, int ccCoaches) {
        List<Seat> seats = new ArrayList<>();
        // Generate EC (Executive Chair Car)
        for (int i = 1; i <= ecCoaches; i++) {
            for (int s = 1; s <= 56; s++) { // usually 56 seats in EC
                seats.add(buildSeat(train, "E" + i, "EC-" + s, Seat.TravelClass.EC, "Window/Aisle"));
            }
        }
        // Generate CC (Chair Car)
        for (int i = 1; i <= ccCoaches; i++) {
            for (int s = 1; s <= 78; s++) { // usually 78 seats in CC
                seats.add(buildSeat(train, "C" + i, "CC-" + s, Seat.TravelClass.CC, "Window/Middle/Aisle"));
            }
        }
        seatDAO.saveAll(seats); // Batch insert is optimal for MongoDB
    }

    private void generateSeatsForSleeperTrain(Train train, int sl, int ac3, int ac2, int ac1) {
        List<Seat> seats = new ArrayList<>();
        // SL
        for (int i = 1; i <= sl; i++) {
            for (int s = 1; s <= 72; s++) seats.add(buildSeat(train, "S" + i, String.valueOf(s), Seat.TravelClass.SLEEPER, getBerthType(s)));
        }
        // 3A
        for (int i = 1; i <= ac3; i++) {
            for (int s = 1; s <= 72; s++) seats.add(buildSeat(train, "B" + i, String.valueOf(s), Seat.TravelClass.AC_3_TIER, getBerthType(s)));
        }
        // 2A
        for (int i = 1; i <= ac2; i++) {
            for (int s = 1; s <= 54; s++) seats.add(buildSeat(train, "A" + i, String.valueOf(s), Seat.TravelClass.AC_2_TIER, getBerthType(s)));
        }
        // 1A
        for (int i = 1; i <= ac1; i++) {
            for (int s = 1; s <= 24; s++) seats.add(buildSeat(train, "H" + i, String.valueOf(s), Seat.TravelClass.AC_1_TIER, "Cabin/Coupe"));
        }
        seatDAO.saveAll(seats);
    }

    private String getBerthType(int seatNo) {
        int mod = seatNo % 8;
        return switch (mod) {
            case 1, 4 -> "Lower Berth";
            case 2, 5 -> "Middle Berth";
            case 3, 6 -> "Upper Berth";
            case 7 -> "Side Lower Berth";
            case 0 -> "Side Upper Berth";
            default -> "Window";
        };
    }

    private Seat buildSeat(Train train, String coach, String number, Seat.TravelClass tClass, String type) {
        return Seat.builder()
                .train(train)
                .coachNumber(coach)
                .seatNumber(number)
                .travelClass(tClass)
                .status(Seat.SeatStatus.AVAILABLE)
                .seatType(type)
                .build();
    }
}

package com.railway.booking.service;

import com.railway.booking.model.Seat;
import com.railway.booking.model.Train;
import org.springframework.stereotype.Service;

/**
 * SERVICE: FareService — Calculate fare (as in MVC diagram)
 * Uses Indian Railways fare formula based on distance and class.
 */
@Service
public class FareService {

    // Base fare per km per class (₹) — approximate Indian Railways rates
    private static final double SL_RATE  = 0.48;  // Sleeper
    private static final double AC3_RATE = 1.21;  // AC 3 Tier
    private static final double AC2_RATE = 1.75;  // AC 2 Tier
    private static final double AC1_RATE = 2.98;  // AC First Class
    private static final double CC_RATE  = 1.21;  // Chair Car

    private static final double RESERVATION_CHARGE = 40.0;
    private static final double SUPERFAST_CHARGE   = 45.0;  // for express/rajdhani
    private static final double GST_RATE           = 0.05;  // 5% GST on AC classes

    /**
     * Calculate base fare for a given train and travel class.
     */
    public double calculateFare(Train train, Seat.TravelClass travelClass) {
        double distance = train.getDistanceKm();
        if (distance <= 0) distance = 500; // fallback for missing data

        double baseFare = distance * getRatePerKm(travelClass);
        double fare = baseFare + RESERVATION_CHARGE;

        // Add superfast surcharge for premium trains
        if (isPremiumTrain(train.getTrainType())) {
            fare += SUPERFAST_CHARGE;
        }

        // GST on AC classes
        if (isAcClass(travelClass)) {
            fare *= (1 + GST_RATE);
        }

        return Math.round(fare);
    }

    /** Calculate fare for a specific number of passengers with concessions */
    public double calculateTotalFare(Train train, Seat.TravelClass cls, int passengerCount,
                                     boolean hasSeniorCitizen) {
        double perPersonFare = calculateFare(train, cls);

        // Senior citizen concession: 40% off for men ≥60, 50% for women ≥58
        if (hasSeniorCitizen && (cls == Seat.TravelClass.SLEEPER ||
                                  cls == Seat.TravelClass.AC_3_TIER ||
                                  cls == Seat.TravelClass.AC_2_TIER)) {
            perPersonFare *= 0.6; // simplified: 40% discount
        }

        return Math.round(perPersonFare * passengerCount);
    }

    private double getRatePerKm(Seat.TravelClass cls) {
        return switch (cls) {
            case SLEEPER    -> SL_RATE;
            case AC_3_TIER  -> AC3_RATE;
            case AC_2_TIER  -> AC2_RATE;
            case AC_1_TIER  -> AC1_RATE;
            case CC         -> CC_RATE;
            default         -> SL_RATE;
        };
    }

    private boolean isPremiumTrain(Train.TrainType type) {
        return type == Train.TrainType.RAJDHANI ||
               type == Train.TrainType.SHATABDI ||
               type == Train.TrainType.DURONTO  ||
               type == Train.TrainType.VANDE_BHARAT;
    }

    private boolean isAcClass(Seat.TravelClass cls) {
        return cls == Seat.TravelClass.AC_1_TIER ||
               cls == Seat.TravelClass.AC_2_TIER ||
               cls == Seat.TravelClass.AC_3_TIER ||
               cls == Seat.TravelClass.CC;
    }
}

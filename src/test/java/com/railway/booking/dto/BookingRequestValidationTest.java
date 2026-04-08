package com.railway.booking.dto;

import com.railway.booking.model.Seat;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void bookingRequest_shouldFail_whenJourneyDateIsPast() {
        BookingRequest request = baseRequest();
        request.setJourneyDate(LocalDate.now().minusDays(1));

        Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("journeyDate")));
    }

    @Test
    void bookingRequest_shouldFail_whenPassengerCountExceedsLimit() {
        BookingRequest request = baseRequest();
        request.setPassengers(List.of(
                passenger("A", 20, "MALE"),
                passenger("B", 21, "FEMALE"),
                passenger("C", 22, "MALE"),
                passenger("D", 23, "FEMALE"),
                passenger("E", 24, "MALE"),
                passenger("F", 25, "FEMALE"),
                passenger("G", 26, "OTHER")
        ));

        Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("passengers")));
    }

    @Test
    void bookingRequest_shouldPass_whenRequestIsValid() {
        BookingRequest request = baseRequest();

        Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    private BookingRequest baseRequest() {
        BookingRequest request = new BookingRequest();
        request.setTrainId("T1");
        request.setJourneyDate(LocalDate.now().plusDays(1));
        request.setBoardingStation("New Delhi");
        request.setDestinationStation("Mumbai Central");
        request.setTravelClass(Seat.TravelClass.SLEEPER);
        request.setPaymentMethod("UPI");
        request.setPassengers(List.of(passenger("Rahul Sharma", 30, "MALE")));
        return request;
    }

    private BookingRequest.PassengerDto passenger(String name, int age, String gender) {
        BookingRequest.PassengerDto dto = new BookingRequest.PassengerDto();
        dto.setName(name);
        dto.setAge(age);
        dto.setGender(gender);
        return dto;
    }
}

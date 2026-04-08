package com.railway.booking.dao;

import com.railway.booking.model.Seat;
import com.railway.booking.model.Train;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatDAO extends MongoRepository<Seat, String> {

    List<Seat> findByTrainAndTravelClassAndStatus(Train train, Seat.TravelClass travelClass, Seat.SeatStatus status);

    @Query(value = "{ 'train.$id': ?0, 'travelClass': ?1, 'status': 'AVAILABLE' }", count = true)
    int countAvailableSeats(String trainId, Seat.TravelClass travelClass);

    @Query(value = "{ 'train.$id': ?0, 'travelClass': ?1, 'status': 'WAITLIST' }", count = true)
    int countWaitlistedSeats(String trainId, Seat.TravelClass travelClass);

    @Query("{ 'train.$id': ?0, 'travelClass': ?1, 'status': 'AVAILABLE' }")
    Optional<Seat> findFirstAvailableSeat(String trainId, String travelClass);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1 } }")
    void updateSeatStatus(String seatId, Seat.SeatStatus status);

    List<Seat> findByTrain(Train train);

    List<Seat> findByTrainAndTravelClass(Train train, Seat.TravelClass travelClass);
}

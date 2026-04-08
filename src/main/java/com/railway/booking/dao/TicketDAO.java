package com.railway.booking.dao;

import com.railway.booking.model.Ticket;
import com.railway.booking.model.Train;
import com.railway.booking.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketDAO extends MongoRepository<Ticket, String> {

    Optional<Ticket> findByPnr(String pnr);

    List<Ticket> findByUser(User user);

    List<Ticket> findByUserOrderByBookedAtDesc(User user);

    List<Ticket> findByTrain(Train train);

    List<Ticket> findByStatus(Ticket.TicketStatus status);

    @Query("{ 'user.$id': ?0, 'status': { $ne: 'CANCELLED' } }")
    List<Ticket> findActiveTicketsByUser(String userId);

    @Query(value = "{ 'train.$id': ?0, 'journeyDate': ?1, 'travelClass': ?2, 'status': { $ne: 'CANCELLED' } }", count = true)
    long countBookingsForTrainClassDate(String trainId, LocalDate date, Ticket.TicketStatus cls);

    @Query("{ 'journeyDate': ?0, 'status': 'CONFIRMED' }")
    List<Ticket> findConfirmedTicketsByDate(LocalDate date);

    boolean existsByPnr(String pnr);
}

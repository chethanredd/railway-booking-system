package com.railway.booking.dao;

import com.railway.booking.model.Payment;
import com.railway.booking.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDAO extends MongoRepository<Payment, String> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByTicket(Ticket ticket);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query(value = "{ 'status': 'SUCCESS' }")
    long countSuccessfulPayments();
}

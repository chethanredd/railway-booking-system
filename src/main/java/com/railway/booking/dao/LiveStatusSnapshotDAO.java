package com.railway.booking.dao;

import com.railway.booking.model.LiveStatusSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LiveStatusSnapshotDAO extends MongoRepository<LiveStatusSnapshot, String> {

    Optional<LiveStatusSnapshot> findTopByTrainNumberAndJourneyDateOrderByFetchedAtDesc(String trainNumber, LocalDate journeyDate);
}

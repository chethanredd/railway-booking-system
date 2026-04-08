package com.railway.booking.dao;

import com.railway.booking.model.Train;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainDAO extends MongoRepository<Train, String> {

    Optional<Train> findByTrainNumber(String trainNumber);

    boolean existsByTrainNumber(String trainNumber);

    @Query("{ 'source': { $regex: ?0, $options: 'i' }, 'destination': { $regex: ?1, $options: 'i' }, 'active': true }")
    List<Train> findBySourceAndDestination(String source, String destination);

    @Query("{ '$or': [ { 'trainName': { $regex: ?0, $options: 'i' } }, { 'trainNumber': { $regex: ?0, $options: 'i' } } ] }")
    List<Train> searchTrains(String query);

    List<Train> findByTrainType(Train.TrainType type);

    List<Train> findByActive(boolean active);
}

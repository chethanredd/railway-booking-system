package com.railway.booking.model;

import com.railway.booking.dto.TrainStatusDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "live_status_snapshots")
public class LiveStatusSnapshot {

    @Id
    private String id;

    @Indexed
    private String trainNumber;

    @Indexed
    private LocalDate journeyDate;

    private String source; // LIVE | CACHED | SCHEDULED

    private LocalDateTime fetchedAt = LocalDateTime.now();

    private TrainStatusDTO payload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public void setJourneyDate(LocalDate journeyDate) {
        this.journeyDate = journeyDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public TrainStatusDTO getPayload() {
        return payload;
    }

    public void setPayload(TrainStatusDTO payload) {
        this.payload = payload;
    }
}

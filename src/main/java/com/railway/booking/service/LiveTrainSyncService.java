package com.railway.booking.service;

import com.railway.booking.dao.TrainDAO;
import com.railway.booking.model.Train;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LiveTrainSyncService {

    private static final Logger log = LoggerFactory.getLogger(LiveTrainSyncService.class);

    private final TrainDAO trainDAO;

    // This key must be replaced in application.properties once the user registers on RapidAPI
    @Value("${api.rapidapi.key:YOUR_RAPID_API_KEY_HERE}")
    private String rapidApiKey;

    @Value("${api.rapidapi.host:irctc1.p.rapidapi.com}")
    private String rapidApiHost;

    public LiveTrainSyncService(TrainDAO trainDAO) {
        this.trainDAO = trainDAO;
    }

    /**
     * Executes at 2:00 AM every day to wipe stale caches and pull the latest National Matrix
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncLiveTrains() {
        if ("YOUR_RAPID_API_KEY_HERE".equals(rapidApiKey)) {
            log.warn("RapidAPI Key is not configured. Skipping live train synchronization.");
            return;
        }

        log.info("Starting nightly Live Train Synchronization from RapidAPI...");
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", rapidApiHost);

            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

            // Mock endpoint representing a real IRCTC wrapper aggregate. 
            // Replace with actual provider's URI once authenticated.
            String url = "https://" + rapidApiHost + "/api/v1/searchTrain?query=NDLS";
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseAndPersist(response.getBody());
            } else {
                log.error("RapidAPI responded with non-200 status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Critical failure executing Live Train Sync. Network issue or rate-limit hit.", e);
        }
    }

    private void parseAndPersist(String jsonBody) {
        try {
            JSONObject root = new JSONObject(jsonBody);
            JSONArray trainsArray = root.getJSONArray("data");

            List<Train> newTrains = new ArrayList<>();

            for (int i = 0; i < trainsArray.length(); i++) {
                JSONObject tObj = trainsArray.getJSONObject(i);
                
                Train newTrain = Train.builder()
                        .trainNumber(tObj.optString("train_num"))
                        .trainName(tObj.optString("name"))
                        .source(tObj.optString("train_from"))
                        .destination(tObj.optString("train_to"))
                        .departureTime(LocalTime.parse(tObj.optString("depart_time", "06:00")))
                        .arrivalTime(LocalTime.parse(tObj.optString("arrive_time", "22:00")))
                        .durationMinutes(tObj.optInt("duration_m", 500))
                        .distanceKm(tObj.optDouble("distance", 1000.0))
                        .runningDays(tObj.optString("days", "MON,TUE,WED,THU,FRI,SAT,SUN"))
                        .trainType(Train.TrainType.SUPERFAST_EXPRESS) 
                        .active(true)
                        .build();

                newTrains.add(newTrain);
            }

            // In production, we'd want to meticulously merge. For demo purposes, we log.
            log.info("Successfully parsed {} live trains from API. Persisting to MongoDB.", newTrains.size());
            trainDAO.saveAll(newTrains);
            log.info("Trains synchronized successfully.");

        } catch (Exception e) {
            log.error("Failed to parse JSON payload from Live Train API", e);
        }
    }
}

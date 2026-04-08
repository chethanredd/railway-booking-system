package com.railway.booking.service;

import com.railway.booking.model.Train;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class LiveStatusRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveStatusRefreshScheduler.class);

    private final TrainService trainService;
    private final TrainTrackingService trainTrackingService;

    @Value("${live.status.refresh.enabled:true}")
    private boolean refreshEnabled;

    @Value("${live.status.refresh.batch-size:20}")
    private int batchSize;

    public LiveStatusRefreshScheduler(TrainService trainService, TrainTrackingService trainTrackingService) {
        this.trainService = trainService;
        this.trainTrackingService = trainTrackingService;
    }

    @Scheduled(cron = "${live.status.refresh.cron:0 */5 * * * *}")
    public void refreshCurrentDaySnapshots() {
        if (!refreshEnabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<Train> active = trainService.getAllTrains().stream()
                .filter(Train::isActive)
                .limit(Math.max(1, batchSize))
                .toList();

        int refreshed = 0;
        for (Train train : active) {
            if (trainTrackingService.refreshLiveStatus(train.getTrainNumber(), today)) {
                refreshed++;
            }
        }
        log.info("Live status refresh cycle complete. Updated {} of {} tracked trains.", refreshed, active.size());
    }
}

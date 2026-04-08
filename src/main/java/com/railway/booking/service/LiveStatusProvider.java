package com.railway.booking.service;

import com.railway.booking.dto.TrainStatusDTO;
import com.railway.booking.model.Train;

import java.time.LocalDate;
import java.util.Optional;

public interface LiveStatusProvider {

    Optional<TrainStatusDTO> fetchLiveStatus(Train train, LocalDate journeyDate);
}

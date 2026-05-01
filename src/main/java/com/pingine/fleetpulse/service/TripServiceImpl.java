package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Override
    public TripResponse getLastTrip(String vehicleId) {
        List<Trip> trips = tripDetector.detect(telemetryRepository.findById(vehicleId).stream().toList());
        Trip lastTripById = trips.get(trips.size() - 1);
        return TripResponse.builder()
                .vehicle(vehicleService.getById(vehicleId))
                .startedAt(lastTripById.getStartedAt())
                .endedAt(lastTripById.getEndedAt())
                .avgSpeedKph(lastTripById.getAvgSpeedKph())
                .distanceKm(lastTripById.getDistanceKm())
                .pointCount(lastTripById.getPoints().size())
                .points(lastTripById.getPoints().stream().map(point -> TripResponse.PointDto.builder()
                        .ts(point.getTs())
                        .lat(point.getLat())
                        .lon(point.getLon())
                        .speedKph(point.getSpeedKph()).build()).toList()
                ).build();
    }
}

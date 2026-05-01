package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Override
    public TripResponse getLastTrip(String vehicleId) {

        List<TelemetryPoint> telemetryPoints = telemetryRepository.findRecentPoints(vehicleId, 5);
        if (telemetryPoints.isEmpty()) {
            throw new VehicleNotFoundException("404");
        }
        List<Trip> trips = tripDetector.detect(telemetryPoints);
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

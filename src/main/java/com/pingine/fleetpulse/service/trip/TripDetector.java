package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
public class TripDetector {

    public List<Trip> detect(List<TelemetryPoint> points) {
        if (points == null)
            throw new UnsupportedOperationException("Implement me.");
        points.stream().sorted(Comparator.comparing(TelemetryPoint::getId)
                .thenComparing(TelemetryPoint::getTs));
        return parseTelemetryPointForTrip(points);
    }

    private List<Trip> parseTelemetryPointForTrip(List<TelemetryPoint> points) {
        List<Trip> resultParseTrip = null;
        Trip trip = null;
        for (TelemetryPoint point : points) {
            if (trip == null) {
                trip = Trip.builder().vehicleId(point.getVehicleId()).startedAt(Instant.from(point.getTs())).build();
            }
            trip.getPoints().add(Trip.TripPoint
                    .builder()
                    .ts(Instant.from(point.getTs()))
                    .lat(point.getLat())
                    .lon(point.getLon())
                    .speedKph(point.getSpeed())
                    .build());
            if (!point.isIgnition()) {
                Double avgSpeed = trip.getPoints().stream().map(Trip.TripPoint::getSpeedKph).mapToDouble(Double::doubleValue).average().orElse(0.0);
                resultParseTrip.add(Trip.builder()
                        .vehicleId(trip.getVehicleId())
                        .startedAt(trip.getStartedAt())
                        .endedAt(Instant.from(point.getTs()))
                        .avgSpeedKph(avgSpeed)
                        .distanceKm(trip.getStartedAt().until(point.getTs(), ChronoUnit.HOURS) * avgSpeed)
                        .points(trip.getPoints())
                        .build());
                trip = null;
            }

        }
        return resultParseTrip;
    }
}

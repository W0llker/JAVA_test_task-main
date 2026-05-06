package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
        List<Trip> resultParseTrip = new ArrayList<>();
        List<Trip.TripPoint> tripPoints = new ArrayList<>();
        Trip trip = null;
        for (TelemetryPoint point : points) {
            if (trip == null) {
                trip = Trip.builder()
                        .vehicleId(point.getVehicleId())
                        .startedAt(point.getTs().toInstant(ZoneOffset.UTC))
                        .build();

            }
            tripPoints.add(Trip.TripPoint
                    .builder()
                    .ts(point.getTs().toInstant(ZoneOffset.UTC))
                    .lat(point.getLat())
                    .lon(point.getLon())
                    .speedKph(point.getSpeed())
                    .build());
            if (!point.isIgnition()) {
                Double avgSpeed = tripPoints.stream().map(Trip.TripPoint::getSpeedKph).mapToDouble(Double::doubleValue).average().orElse(0.0);
                resultParseTrip.add(Trip.builder()
                        .vehicleId(trip.getVehicleId())
                        .startedAt(trip.getStartedAt())
                        .endedAt(point.getTs().toInstant(ZoneOffset.UTC))
                        .avgSpeedKph(avgSpeed)
                        .distanceKm(((double) Duration.between(trip.getStartedAt(), point.getTs().toInstant(ZoneOffset.UTC))
                                .toMinutes() / 60 * avgSpeed))
                        .points(tripPoints)
                        .build());
                trip = null;
            }

        }
        return resultParseTrip;
    }
}

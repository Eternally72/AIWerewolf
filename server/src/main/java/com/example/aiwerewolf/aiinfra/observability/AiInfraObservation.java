package com.example.aiwerewolf.aiinfra.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class AiInfraObservation {
    private final ObservationRegistry observationRegistry;

    public AiInfraObservation(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public <T> T observe(String name, Supplier<T> supplier) {
        return Observation.createNotStarted(name, observationRegistry)
                .observe(supplier);
    }
}

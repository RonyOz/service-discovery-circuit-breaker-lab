package com.futurex.services.FutureXCourseCatalog;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Demuestra que Resilience4JConfig y application.properties configuran cosas distintas.
@SpringBootTest
class Resilience4JConfigTests {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Autowired
    private Resilience4JCircuitBreakerFactory factory;

    @Test
    void annotationsUsePropertiesNotCustomizer() {
        // La instancia que usan las anotaciones de CatalogController
        CircuitBreakerConfig config = registry.circuitBreaker("courseService").getCircuitBreakerConfig();

        assertEquals(10, config.getSlidingWindowSize());
        assertEquals(5, config.getMinimumNumberOfCalls());
        assertEquals(5000L, config.getWaitIntervalFunctionInOpenState().apply(1));
    }

    @Test
    void programmaticFactoryUsesCustomizer() {
        // Uso programatico: aqui si aplica Resilience4JConfig
        String result = factory.create("programmaticDemo").run(() -> "ok");
        CircuitBreakerConfig config = factory.getCircuitBreakerRegistry()
                .circuitBreaker("programmaticDemo").getCircuitBreakerConfig();

        assertEquals("ok", result);
        assertEquals(2, config.getSlidingWindowSize());
        assertEquals(1000L, config.getWaitIntervalFunctionInOpenState().apply(1));
    }
}

package com.futurex.services.FutureXCourseCatalog;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// Paso 5 (opcional) del enunciado.
// Esta configuracion SOLO aplica al uso programatico de Spring Cloud CircuitBreaker:
//     circuitBreakerFactory.create("id").run(() -> llamada, ex -> fallback)
// NO afecta a las anotaciones @CircuitBreaker/@Retry de CatalogController, que leen
// resilience4j.circuitbreaker.instances.courseService.* de application.properties
// (ventana 10, espera 5s). Verificado en Resilience4JConfigTests.
@Configuration
public class Resilience4JConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> globalCustomConfiguration() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slidingWindowSize(2)
                .build();
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerConfig)
                .build());
    }
}

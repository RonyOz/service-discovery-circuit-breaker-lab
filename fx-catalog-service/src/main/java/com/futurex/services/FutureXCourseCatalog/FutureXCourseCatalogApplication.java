package com.futurex.services.FutureXCourseCatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@SpringBootApplication
public class FutureXCourseCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(FutureXCourseCatalogApplication.class, args);
	}

	// TimeLimiter deja de esperar a los 2s, pero no puede interrumpir una lectura de socket
	// bloqueada. El readTimeout (mayor que el del TimeLimiter) libera el hilo del Bulkhead.
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.setConnectTimeout(Duration.ofSeconds(1))
				.setReadTimeout(Duration.ofSeconds(3))
				.build();
	}

}

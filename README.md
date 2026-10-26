# FutureX Service Discovery and Circuit Breaker Lab

Demo de Service Discovery con Eureka y resiliencia con Resilience4j usando tres aplicaciones Spring Boot:

- Eureka Server: `http://localhost:8761`
- Course Service: `http://localhost:8001`
- Catalog Service: `http://localhost:8002`

## Ejecutar con Docker Compose

Requisitos: Docker con Compose v2.

```bash
docker compose up --build
```

Verificar el estado:

```bash
docker compose ps
curl -fsS http://localhost:8761/
curl -fsS http://localhost:8001/courses
curl -fsS http://localhost:8002/catalog
curl -fsS http://localhost:8002/actuator/health
```

Detener los servicios:

```bash
docker compose down
```

La base H2 de `fx-course-service` es en memoria; sus cursos de ejemplo se vuelven a cargar al reiniciar el contenedor.

## Postman

Importar:

- `postman/futurex-service-discovery.postman_collection.json`
- `postman/futurex-local.postman_environment.json` para ejecución manual con los tres procesos locales.
- `postman/futurex-docker.postman_environment.json` para ejecución con Compose.

La colección tiene 4 carpetas:

- **Eureka**: panel y registro de aplicaciones.
- **Course service**: CRUD completo del proveedor (GET, POST, DELETE) — sin resiliencia, es la línea base de la Parte 1.
- **Catalog service**: los 3 endpoints del consumidor y el escenario manual de fallback.
- **Resilience monitoring**: evidencia de los 5 patrones de Resilience4j vía Actuator (circuit breaker, retry, rate limiter, bulkhead, time limiter — estado y eventos).

Ejecutar primero `Eureka`, `Course service` y `Catalog service`. La request `Fallback after stopping course (manual scenario)` requiere:

```bash
docker compose stop course-service
```

Después de ejecutar varias veces `/catalog`, correr toda la carpeta `Resilience monitoring` para ver el estado del circuito y sus eventos. Para comprobar recuperación:

```bash
docker compose start course-service
```

Esperar a que vuelva a registrarse en Eureka, repetir las requests normales del catálogo y volver a correr `Resilience monitoring`.

RateLimiter y Bulkhead necesitan concurrencia real (varias llamadas en el mismo segundo), que Postman no genera porque ejecuta las requests una por una. Para esos dos patrones, dispara la ráfaga desde una terminal (ver `docs/guion-demo.md`) y después usa las requests de `Resilience monitoring` para ver los eventos resultantes.

## Ejecución local sin Docker

En tres terminales:

```bash
cd fx-discovery-server && ./mvnw spring-boot:run
cd fx-course-service && ./mvnw spring-boot:run
cd fx-catalog-service && ./mvnw spring-boot:run
```

La configuración predeterminada usa `http://localhost:8761/eureka/`. Para cambiarla:

```bash
EUREKA_URL=http://otro-host:8761/eureka/ ./mvnw spring-boot:run
```

## Flujo de la demo

1. Eureka registra `fx-course-service` y `fx-catalog-service`.
2. Catalog consulta Eureka por el nombre lógico `fx-course-service`.
3. Catalog llama a Course mediante `RestTemplate`.
4. Si Course falla, Resilience4j aplica Retry, Circuit Breaker, RateLimiter, TimeLimiter y Bulkhead según corresponda.
5. El fallback devuelve una respuesta degradada y Actuator expone estado/eventos.

La implementación usa Resilience4j como reemplazo moderno de Hystrix; el taller original menciona Hystrix, pero esa biblioteca no es compatible con el stack Spring Boot 3 utilizado aquí.

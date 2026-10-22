# TODO — Taller Service Discovery + Circuit Breaker

Basado en `enunciado.md` (PDF + consigna). Estado al 2026-10-22.

Leyenda: `[x]` hecho · `[ ]` pendiente · *(opcional)* según el enunciado

---

## Parte 1 — Service Discovery (40% de la nota)

### Guía paso a paso
- [x] 1. Tener los 3 repos (`fx-discovery-server`, `fx-course-service`, `fx-catalog-service`). Son idénticos a los de jrquinte.
- [x] 2. Verificar configuración y ejecutar `FutureXEurekaServerApplication` (:8761)
- [x] 3. Verificar configuración y ejecutar `FutureXCourseAppApplication` (:8001)
- [x] 4. Verificar configuración y ejecutar `FutureXCourseCatalogApplication` (:8002)
- [x] 5. Entender la interacción: `getNextServerFromEureka` → `getHomePageUrl` → `RestTemplate`
- [x] 6. Probar endpoints: `/`, `/catalog`, `/firstcourse` responden 200
- [x] 7. Panel de Eureka muestra `FX-COURSE-SERVICE` y `FX-CATALOG-SERVICE` en UP

### Criterios de calificación
- [x] Explicación de cómo catalog descubre y se comunica con course → `docs/parte1-service-discovery.md`
- [ ] Demostración de los endpoints **en vivo** (guion en la sección 6 del doc)
- [ ] *(recomendado)* Capturas de pantalla del panel de Eureka y de los endpoints para la presentación

---

## Parte 2 — Circuit Breaker con Resilience4j (60% de la nota)

Todo en `fx-catalog-service`. Detalle y evidencia en `docs/parte2-resilience4j.md`.

### Guía paso a paso
- [x] 1. Dependencias en `pom.xml`: `spring-cloud-starter-circuitbreaker-resilience4j` + `spring-boot-starter-aop`
- [x] 2. Instancia `courseService` en `application.properties` con los valores exactos del enunciado
- [x] 3. Métodos de fallback, uno por endpoint:
  - [x] `fallbackGetCatalogHome` (`/`)
  - [x] `fallbackGetCatalog` (`/catalog`)
  - [x] `fallbackGetSpecificCourse` (`/firstcourse`)
- [x] 4. `@CircuitBreaker(name = "courseService")` en los 3 métodos de `CatalogController`
- [x] 5. *(opcional)* `Resilience4JConfig`: incluida con un comentario. Solo aplica al uso programático (`factory.create(...)`), no a las anotaciones; verificado con `Resilience4JConfigTests` (sección 2.3 del doc).
- [x] 6. Probar el Circuit Breaker (verificado):
  - [x] endpoints normales → respuesta real (CLOSED)
  - [x] apagar `fx-course-service` → fallback en vez de 500
  - [x] ≥50% de fallos → OPEN (fallback en 0.00 s, sin llamar a course)
  - [x] a los 5 s → HALF_OPEN
  - [x] reiniciar `fx-course-service` → CLOSED
- [x] 7. *(opcional)* Monitoreo con Actuator: `/actuator/health`, `/actuator/circuitbreakers`, `/actuator/circuitbreakerevents`, `/actuator/retries`

### Criterios de calificación
- [x] Correcta configuración de Resilience4j en el proyecto
- [x] Implementación adecuada de métodos de fallback
- [x] Uso apropiado de las anotaciones de Resilience4j en el controlador
- [x] Configuración adecuada de propiedades en `application.properties`
- [x] Circuit Breaker **y otros patrones de resiliencia** funcionando:
  - [x] `@Retry`: 3 intentos cada 500 ms antes del fallback; no reintenta con el circuito OPEN
  - [ ] *(opcional, si quieren sumar)* `@RateLimiter` / `@Bulkhead` / `@TimeLimiter`
- [x] Explicación de cómo **Resilience4j** mejora la resiliencia → sección 4 del doc
- [x] Explicación de cómo **Hystrix** mejora la resiliencia + comparación → sección 6 del doc
- [ ] Demostración **en vivo** (guion en la sección 7 del doc)

---

## Entregables

- [x] 1. **Código fuente** de la implementación de Resilience4j: en `main`
- [ ] 2. **Presentación** (el contenido ya está en los dos docs). Secciones:
  - [ ] Service Discovery: arquitectura, diagrama, flujo (`docs/parte1-service-discovery.md`)
  - [ ] Problema: course caído → catalog responde 500
  - [ ] Circuit Breaker: estados CLOSED / OPEN / HALF_OPEN, configuración y diseño Retry + CB
  - [ ] Evidencia (tiempos 1.03 s → 0.00 s, eventos de transición)
  - [ ] Resilience4j vs Hystrix
- [ ] 3. **Demostración** en vivo: Parte 1 + Parte 2

---

## Housekeeping del repo
- [x] `enunciado.md` y `docs/` creados
- [x] Commit y push a `main`
- [x] `chmod +x` a los `mvnw` (ahora `./mvnw` funciona directo)

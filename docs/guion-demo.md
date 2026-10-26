# Guion unificado de demo — Service Discovery + Circuit Breaker

> Notas del presentador. Combina la arquitectura y las decisiones de diseño (`docs/parte1-service-discovery.md`, `docs/parte2-resilience4j.md`) con los pasos exactos para ejecutar la demo en vivo con Docker Compose y Postman.
>
> Verificado en vivo el 2026-10-26 con `docker compose up --build`: registro en Eureka, catálogo normal, caída de `course-service`, transición CLOSED → OPEN → HALF_OPEN → CLOSED, y recuperación automática.

---

## 0. Preparación (antes de presentar)

```bash
docker compose up --build -d
docker compose ps        # los 3 deben quedar "healthy" (~30-40 s)
```

Postman:
1. Importar `postman/futurex-service-discovery.postman_collection.json`.
2. Importar `postman/futurex-docker.postman_environment.json` y `postman/futurex-local.postman_environment.json`.
3. Seleccionar el entorno **FutureX - Docker Compose** (arriba a la derecha).

Tener a la mano, en pestañas separadas:
- `http://localhost:8761` (panel de Eureka)
- Postman con la colección abierta
- Una terminal para los comandos de `docker compose stop/start`

---

## 1. El mensaje central (30 segundos)

> "Tenemos tres microservicios Spring Boot. Uno de ellos, el catálogo, depende de otro, el de cursos, para funcionar. Voy a mostrar dos cosas: cómo el catálogo **encuentra** al servicio de cursos sin conocer su dirección de antemano (Service Discovery con Eureka), y qué pasa cuando ese servicio **falla** — y cómo Resilience4j evita que la falla se propague (Circuit Breaker y patrones de resiliencia)."

---

## 2. Arquitectura (parte 1 — 40% de la nota)

| Servicio | Puerto | Rol |
|---|---|---|
| `fx-discovery-server` | 8761 | Servidor Eureka: registro de servicios |
| `fx-course-service` | 8001 | Proveedor de cursos (JPA + H2 en memoria) |
| `fx-catalog-service` | 8002 | Consumidor: arma el catálogo pidiendo cursos |

Mostrar el diagrama de secuencia de `docs/parte1-service-discovery.md` (sección 1): registro → heartbeat → `getNextServerFromEureka` → `RestTemplate`.

**Por qué Eureka y no una URL fija** (tabla de la sección 2.5 del doc): si el servicio de cursos cambia de host/puerto no hay que recompilar el catálogo; hay balanceo round-robin si hay varias instancias; y Eureka retira instancias muertas automáticamente.

### Demo en vivo
1. Abrir `http://localhost:8761` → mostrar `FX-COURSE-SERVICE` y `FX-CATALOG-SERVICE` en **UP**.
2. En Postman, correr la carpeta **Eureka** (2 requests) → deben pasar los tests automáticos.
3. Correr la carpeta **Course service** (Home, List courses, Get first course).
4. Correr la carpeta **Catalog service** → primeras 3 requests (`Catalog home`, `Catalog courses`, `First catalog course`): todas en verde, porque el catálogo encontró a `course` vía Eureka.
5. Abrir `CatalogController.java` y señalar las 3 líneas clave (sección 2.3 del doc): `client.getNextServerFromEureka(...)` → `getHomePageUrl()` → `restTemplate.getForObject(...)`.

---

## 3. El problema que motiva la parte 2

> "¿Qué pasa si `course-service` se cae?"

Sin resiliencia, el catálogo respondería **500** y seguiría intentando llamar a un servicio muerto en cada request (ver sección 5 de `parte1-service-discovery.md`). Eso, en un sistema real con muchos consumidores, agota hilos y conexiones: **falla en cascada**.

---

## 4. Qué patrones se implementaron y por qué (parte 2 — 60% de la nota)

Cadena de aspectos sobre cada endpoint del catálogo (`docs/parte2-resilience4j.md`, sección 3.2):

```
Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead(thread pool) → llamada real a course
```

| Patrón | Qué resuelve | Decisión de diseño clave |
|---|---|---|
| **Retry** | Fallas transitorias (un timeout puntual) | 3 intentos, 500 ms de espera; ignora rechazos de las capas internas (no reintenta contra un circuito ya abierto) |
| **Circuit Breaker** | Fallas sostenidas → corta y falla rápido | ≥50% de fallos en 10 llamadas (mín. 5) abre el circuito; 5 s en OPEN antes de probar en HALF_OPEN |
| **RateLimiter** | Picos de tráfico hacia `course` | 5 llamadas/s; exceso rechazado al instante (`timeoutDuration=0`), sin ocupar hilos de Tomcat |
| **Bulkhead** (thread pool) | Aislar recursos: un `course` colgado no debe tumbar el catálogo entero | Thread pool (no semáforo) porque TimeLimiter necesita que la llamada corra en otro hilo; 2 hilos + 1 en cola |
| **TimeLimiter** | Un servicio **lento** es peor que uno caído | Corta a los 2 s; por eso los métodos del controlador devuelven `CompletableFuture` |

**Por qué el fallback va en `@Retry` y no en `@CircuitBreaker`** (decisión de diseño más sutil, sección 2.3 del doc): si el fallback estuviera en una capa interna, esa capa "tragaría" la excepción antes de que Retry la viera, y Retry nunca reintentaría. Vale la pena mencionarla porque demuestra que se entendió el orden de los aspectos, no solo que se copiaron anotaciones.

**Por qué Resilience4j y no Hystrix** (el enunciado original menciona Hystrix): Hystrix está en mantenimiento desde 2018 y Spring Cloud lo eliminó en 2020; no compila en Spring Boot 3 / Jakarta EE. Resilience4j es el reemplazo oficial recomendado por Netflix y Spring: modular (se usa solo lo necesario), más liviano, y con soporte activo. Tabla comparativa completa en `docs/parte2-resilience4j.md` sección 6.

### Demo en vivo — Circuit Breaker

1. Postman → **Catalog service → Circuit breaker health**: mostrar `state: CLOSED`.
2. Terminal:
   ```bash
   docker compose stop course-service
   ```
3. Postman → correr **"Fallback after stopping course (manual scenario)"** 2-3 veces seguidas:
   - 1ª vez: tarda ~1-4 s (Retry agotando 3 intentos) y responde `Fallback: ... (retries exhausted: ...)`.
   - 2ª/3ª vez: responde **instantáneo** con `Fallback: ... (circuit OPEN, call not attempted)`.
4. Postman → **Circuit breaker health**: `state: OPEN`.
5. Postman → **Circuit breaker events**: mostrar las transiciones (`NOT_PERMITTED`, etc.).
6. Terminal:
   ```bash
   docker compose start course-service
   ```
7. Esperar ~15-20 s (re-registro en Eureka) y volver a correr **Circuit breaker health** un par de veces: se ve `OPEN → HALF_OPEN → CLOSED`.
8. Volver a correr **Catalog courses**: respuesta normal.

### (Opcional, si hay tiempo) RateLimiter, Bulkhead, TimeLimiter

Con los 3 servicios sanos:

```bash
# RateLimiter: 5 pasan, 2 reciben fallback instantáneo
for i in $(seq 1 7); do curl -s localhost:8002/catalog | head -c 60; echo; done
```

Explicar con `docs/parte2-resilience4j.md` sección 3.2 si preguntan por Bulkhead/TimeLimiter (requieren congelar el proceso con `kill -STOP`, más difícil de mostrar en contenedores — mejor remitir a la evidencia ya documentada con capturas/logs reales).

---

## 5. Cierre (1 minuto)

- Service Discovery: el catálogo nunca "supo" dónde vivía `course`; lo preguntó.
- Sin resiliencia: una caída de `course` tumba al catálogo (500, hilos agotados).
- Con Resilience4j: la caída se **contiene** (Bulkhead), se **reintenta** lo razonable (Retry), se **corta** cuando ya no vale la pena insistir (Circuit Breaker), se **limita** la carga (RateLimiter) y se **evita esperar para siempre** (TimeLimiter).
- Todo esto es observable en `/actuator/health`, `/actuator/circuitbreakers` y los `*events` — no es una caja negra.

## 6. Preguntas que probablemente te hagan

| Pregunta | Respuesta corta (con referencia) |
|---|---|
| ¿Por qué no usaron Hystrix, que es lo que pide el enunciado? | Está deprecado desde 2018, incompatible con Spring Boot 3. Resilience4j es el reemplazo oficial (parte2 §6). |
| ¿Qué pasa si varias instancias de `course-service` están registradas? | `getNextServerFromEureka` las balancea en round-robin (parte1 §2.3). |
| ¿Por qué el fallback está en `@Retry` y no en `@CircuitBreaker`? | Orden de aspectos: si estuviera más adentro, "tragaría" el error antes de que Retry lo viera (parte2 §2.3). |
| ¿Por qué Bulkhead es thread-pool y no semáforo? | TimeLimiter exige que la llamada corra en otro hilo para poder "dejar de esperarla" (parte2 §3.2). |
| ¿Qué pasa si el circuito se abre a mitad de una ráfaga? | Se verificó: el circuito puede abrirse durante los reintentos de la propia request que lo dispara (parte2 §3.1, evidencia real). |

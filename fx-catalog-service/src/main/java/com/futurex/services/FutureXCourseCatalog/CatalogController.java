package com.futurex.services.FutureXCourseCatalog;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@RestController
public class CatalogController {

    @Autowired
    private EurekaClient client;

    @Autowired
    private RestTemplate restTemplate;

    // Resilience4j aplica los aspectos en este orden (las anotaciones estan escritas igual):
    //   Retry( CircuitBreaker( RateLimiter( TimeLimiter( Bulkhead( metodo ) ) ) ) )
    // El fallback va solo en @Retry; si estuviera en una capa interna, Retry nunca veria el error.
    // TimeLimiter necesita un CompletableFuture que corra en otro hilo: por eso el Bulkhead es
    // THREADPOOL (ejecuta el metodo en su pool) y los metodos devuelven CompletableFuture.
    @RequestMapping("/")
    @Retry(name = "courseService", fallbackMethod = "fallbackGetCatalogHome")
    @CircuitBreaker(name = "courseService")
    @RateLimiter(name = "courseService")
    @TimeLimiter(name = "courseService")
    @Bulkhead(name = "courseService", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> getCatalogHome() {
        String courseAppMesage = "";
        //String courseAppURL = "http://localhost:8080/";
        InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service",false);
        String courseAppURL = instanceInfo.getHomePageUrl();
        courseAppMesage = restTemplate.getForObject(courseAppURL,String.class);

        return CompletableFuture.completedFuture("Welcome to FutureX Course Catalog "+courseAppMesage);
    }

    @RequestMapping("/catalog")
    @Retry(name = "courseService", fallbackMethod = "fallbackGetCatalog")
    @CircuitBreaker(name = "courseService")
    @RateLimiter(name = "courseService")
    @TimeLimiter(name = "courseService")
    @Bulkhead(name = "courseService", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> getCatalog() {
        String courses = "";
        //String courseAppURL = "http://localhost:8080/courses";
        InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service",false);
        String courseAppURL = instanceInfo.getHomePageUrl();
        courseAppURL = courseAppURL+"/courses";
        courses = restTemplate.getForObject(courseAppURL,String.class);

        return CompletableFuture.completedFuture("Our courses are "+courses);
    }

    @RequestMapping("/firstcourse")
    @Retry(name = "courseService", fallbackMethod = "fallbackGetSpecificCourse")
    @CircuitBreaker(name = "courseService")
    @RateLimiter(name = "courseService")
    @TimeLimiter(name = "courseService")
    @Bulkhead(name = "courseService", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> getSpecificCourse() {
        Course course = new Course();
        //String courseAppURL = "http://localhost:8080/1";
        InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service",false);
        String courseAppURL = instanceInfo.getHomePageUrl();
        courseAppURL = courseAppURL+"/1";

        course = restTemplate.getForObject(courseAppURL,Course.class);

        return CompletableFuture.completedFuture("Our first course is "+course.getCoursename());
    }

    public CompletableFuture<String> fallbackGetCatalogHome(Exception ex) {
        return CompletableFuture.completedFuture("Welcome to FutureX Course Catalog. Fallback: course service unavailable ("+reason(ex)+")");
    }

    public CompletableFuture<String> fallbackGetCatalog(Exception ex) {
        return CompletableFuture.completedFuture("Fallback: Unable to retrieve course catalog ("+reason(ex)+")");
    }

    public CompletableFuture<String> fallbackGetSpecificCourse(Exception ex) {
        return CompletableFuture.completedFuture("Fallback: Unable to retrieve first course ("+reason(ex)+")");
    }

    private String reason(Throwable ex) {
        // En el camino asincrono la excepcion real puede llegar envuelta
        if (ex instanceof CompletionException && ex.getCause() != null) {
            ex = ex.getCause();
        }
        if (ex instanceof CallNotPermittedException) {
            return "circuit OPEN, call not attempted";
        }
        if (ex instanceof RequestNotPermitted) {
            return "rate limit exceeded, call not attempted";
        }
        if (ex instanceof BulkheadFullException) {
            return "bulkhead full, call not attempted";
        }
        return "retries exhausted: "+ex.getClass().getSimpleName();
    }

}

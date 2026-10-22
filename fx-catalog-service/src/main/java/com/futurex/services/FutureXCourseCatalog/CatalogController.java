package com.futurex.services.FutureXCourseCatalog;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class CatalogController {

    @Autowired
    private EurekaClient client;

    // Retry envuelve al CircuitBreaker: cada intento pasa por el circuito.
    // El fallback va solo en @Retry; si estuviera en @CircuitBreaker, Retry nunca veria el error.
    @RequestMapping("/")
    @Retry(name = "courseService", fallbackMethod = "fallbackGetCatalogHome")
    @CircuitBreaker(name = "courseService")
    public String getCatalogHome() {
        String courseAppMesage = "";
        //String courseAppURL = "http://localhost:8080/";
        RestTemplate restTemplate = new RestTemplate();
        InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service",false);
        String courseAppURL = instanceInfo.getHomePageUrl();
        courseAppMesage = restTemplate.getForObject(courseAppURL,String.class);

        return("Welcome to FutureX Course Catalog "+courseAppMesage);
    }

    @RequestMapping("/catalog")
    @Retry(name = "courseService", fallbackMethod = "fallbackGetCatalog")
    @CircuitBreaker(name = "courseService")
    public String getCatalog() {
        String courses = "";
        //String courseAppURL = "http://localhost:8080/courses";
        InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service",false);
        String courseAppURL = instanceInfo.getHomePageUrl();
        courseAppURL = courseAppURL+"/courses";
        RestTemplate restTemplate = new RestTemplate();
        courses = restTemplate.getForObject(courseAppURL,String.class);

        return("Our courses are "+courses);
    }

    @RequestMapping("/firstcourse")
    @Retry(name = "courseService", fallbackMethod = "fallbackGetSpecificCourse")
    @CircuitBreaker(name = "courseService")
    public String getSpecificCourse() {
        Course course = new Course();
        //String courseAppURL = "http://localhost:8080/1";
        InstanceInfo instanceInfo = client.getNextServerFromEureka("fx-course-service",false);
        String courseAppURL = instanceInfo.getHomePageUrl();
        courseAppURL = courseAppURL+"/1";
        RestTemplate restTemplate = new RestTemplate();

        course = restTemplate.getForObject(courseAppURL,Course.class);

        return("Our first course is "+course.getCoursename());
    }

    public String fallbackGetCatalogHome(Exception ex) {
        return("Welcome to FutureX Course Catalog. Fallback: course service unavailable ("+reason(ex)+")");
    }

    public String fallbackGetCatalog(Exception ex) {
        return("Fallback: Unable to retrieve course catalog ("+reason(ex)+")");
    }

    public String fallbackGetSpecificCourse(Exception ex) {
        return("Fallback: Unable to retrieve first course ("+reason(ex)+")");
    }

    private String reason(Exception ex) {
        if (ex instanceof CallNotPermittedException) {
            return "circuit OPEN, call not attempted";
        }
        return "retries exhausted: "+ex.getClass().getSimpleName();
    }

}

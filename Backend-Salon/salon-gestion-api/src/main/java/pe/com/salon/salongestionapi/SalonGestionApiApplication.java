package pe.com.salon.salongestionapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SalonGestionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalonGestionApiApplication.class, args);
    }

}

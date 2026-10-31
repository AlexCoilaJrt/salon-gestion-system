package pe.com.salon.salongestionapi;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SalonGestionApiApplication {

    @PostConstruct
    public void init() {
        // Fijar zona horaria oficial del sistema en Perú (UTC-5)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SalonGestionApiApplication.class, args);
    }

}

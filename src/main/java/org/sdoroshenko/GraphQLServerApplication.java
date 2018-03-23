package org.sdoroshenko;

import org.sdoroshenko.model.Car;
import org.sdoroshenko.repository.CarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GraphQLServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner startup(CarRepository carRepository) {
        return (args) -> {
            carRepository.save(new Car(null, "S0273VI3748374K", "Honda", "Civic", "2007"));
            carRepository.save(new Car(null, "1FA0273VI483550", "Toyota", "RAV4", "2017"));
        };
    }
}

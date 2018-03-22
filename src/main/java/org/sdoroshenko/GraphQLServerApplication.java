package org.sdoroshenko;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.servlet.GraphQLErrorHandler;
import org.sdoroshenko.exception.GraphQLErrorAdapter;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.repository.CarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class GraphQLServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLServerApplication.class, args);
    }

    @Bean
    public GraphQLErrorHandler errorHandler() {
        return new GraphQLErrorHandler() {
            @Override
            public List<GraphQLError> processErrors(List<GraphQLError> errors) {
                List<GraphQLError> clientErrors = errors.stream()
                    .filter(this::isClientError)
                    .collect(Collectors.toList());

                List<GraphQLError> serverErrors = errors.stream()
                    .filter(e -> !isClientError(e))
                    .map(GraphQLErrorAdapter::new)
                    .collect(Collectors.toList());

                List<GraphQLError> e = new ArrayList<>();
                e.addAll(clientErrors);
                e.addAll(serverErrors);
                return e;
            }

            protected boolean isClientError(GraphQLError error) {
                return !(error instanceof ExceptionWhileDataFetching || error instanceof Throwable);
            }
        };
    }

    @Bean
    public CommandLineRunner startup(CarRepository carRepository) {
        return (args) -> {
            carRepository.save(new Car(null, "S0273VI3748374K", "Honda", "Civic", "2007"));
        };
    }
}

package org.sdoroshenko;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.coxautodev.graphql.tools.GraphQLSubscriptionResolver;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.publisher.MessageGraphqlPublisher;
import org.sdoroshenko.publisher.SocketHandler;
import org.sdoroshenko.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class GraphQLServerApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLServerApplication.class, args);
    }

    @Bean
    public GraphQLQueryResolver query() {
        return new GraphQLQueryResolver() {
            @Autowired
            CarRepository carRepository;

            public Iterable<Car> findAllCars() {
                return carRepository.findAll();
            }
        };
    }

    @Bean
    public GraphQLMutationResolver mutation() {
        return new GraphQLMutationResolver() {
            @Autowired
            CarRepository carRepository;

            public Car newCar(String vin, String make, String model, String year) {
                Car car = new Car(null, vin, make, model, year);
                carRepository.save(car);
                return car;
            }

            public Boolean deleteCar(Long id) {
                carRepository.delete(id);
                return Boolean.TRUE;
            }
        };
    }

    @Bean
    public GraphQLSubscriptionResolver subscription() {
        return new GraphQLSubscriptionResolver() {
            public Message messages() {
                return new Message(2L, "test");
            }
        };
    }

    @Bean
    MessageGraphqlPublisher messageGraphqlPublisher() {
        return new MessageGraphqlPublisher();
    }

    @Bean
    public SocketHandler socketHandler(MessageGraphqlPublisher messageGraphqlPublisher) {
        return new SocketHandler(messageGraphqlPublisher);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler(messageGraphqlPublisher()), "/messages-jt").setAllowedOrigins("*");
    }

    @Bean
    public CommandLineRunner startup(CarRepository carRepository) {
        return (args) -> {
            carRepository.save(new Car(null, "S0273VI3748374K", "Honda", "Civic", "2007"));
            carRepository.save(new Car(null, "1FA0273VI483550", "Toyota", "RAV4", "2017"));
        };
    }
}

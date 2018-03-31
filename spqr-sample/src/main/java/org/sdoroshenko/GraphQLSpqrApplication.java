package org.sdoroshenko;

import com.google.common.collect.Lists;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.publisher.MessageStreamer;
import org.sdoroshenko.publisher.SocketHandlerSPQR;
import org.sdoroshenko.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableWebSocket
public class GraphQLSpqrApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLSpqrApplication.class, args);
    }

    @AllArgsConstructor
    public static class CarGraph {

        private CarRepository carRepository;

        @GraphQLQuery(name = "getAllCars")
        public List<Car> getAllCars() {
            return Lists.newArrayList(carRepository.findAll());
        }

        @GraphQLMutation(name = "addCar")
        public Car addCar(
                @GraphQLArgument(name = "vin") String vin,
                @GraphQLArgument(name = "make") @GraphQLNonNull String make,
                @GraphQLArgument(name = "model") String model,
                @GraphQLArgument(name = "year") String year
        ) {

            return carRepository.save(new Car(null, vin, make, model, year));
        }

    }

    @Bean
    public CarGraph carGraph(CarRepository carRepository) {
        return new CarGraph(carRepository);
    }

    public static class MessageGraph {

        @Autowired
        private MessageStreamer messagePublisher;

        @GraphQLSubscription(name = "messages")
        public Publisher<Message> messages() {
            return messagePublisher.getPublisher();
        }
    }

    @Bean
    public MessageGraph messageGraph() {
        return new MessageGraph();
    }

    @Bean
    public SocketHandlerSPQR socketHandler() {
        return new SocketHandlerSPQR();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler(), "/messages-spqr").setAllowedOrigins("*");
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph, MessageGraph messageGraph) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(carGraph)
                .withOperationsFromSingleton(messageGraph)
                .withValueMapperFactory(new JacksonValueMapperFactory())
                .generate();

        return GraphQL.newGraphQL(schema)
                .queryExecutionStrategy(new BatchedExecutionStrategy())
                .instrumentation(new ChainedInstrumentation(Arrays.asList(
                        new MaxQueryComplexityInstrumentation(200),
                        new MaxQueryDepthInstrumentation(20)
                )))
                .build();
    }

    @Bean
    public CommandLineRunner startup(CarRepository carRepository) {
        return (args) -> {
            carRepository.save(new Car(null, "S0273VI3748374K", "Honda", "Civic", "2007"));
            carRepository.save(new Car(null, "1FA0273VI483550", "Toyota", "RAV4", "2017"));
        };
    }
}

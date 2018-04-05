package org.sdoroshenko;

import com.google.common.collect.Lists;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.batched.Batched;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.model.Conversation;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.publisher.MessageStreamer;
import org.sdoroshenko.publisher.SocketHandlerSPQR;
import org.sdoroshenko.repository.CarRepository;
import org.sdoroshenko.repository.ConversationRepository;
import org.sdoroshenko.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

@SpringBootApplication
@EnableWebSocket
public class GraphQLSpqrApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLSpqrApplication.class, args);
    }

    @Bean
    public CarGraph carGraph(JdbcTemplate jdbcTemplate) {
        return new CarGraph(jdbcTemplate);
    }

    @Bean
    public MessageGraph messageGraph() {
        return new MessageGraph();
    }

    @Bean
    public GraphQL graphQL(MessageGraph messageGraph, ConversationGraph conversationGraph, CarGraph carGraph) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(messageGraph)
                .withOperationsFromSingleton(conversationGraph)
            .withOperationsFromSingleton(carGraph)
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
    public CommandLineRunner startup(
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        CarRepository carRepository
    ) {
        return (args) -> {
            Car car1 = new Car(null, "vin1", null);
            carRepository.save(car1);
            Car car2 = new Car(null, "vin2", null);
            carRepository.save(car2);


            Conversation conversation = new Conversation();
            conversationRepository.save(conversation);
            messageRepository.save(new Message(null, "one", conversation.getId(), car1.getId()));
            messageRepository.save(new Message(null, "two", conversation.getId(), car2.getId()));
        };
    }

    @Bean
    public SocketHandlerSPQR socketHandler() {
        return new SocketHandlerSPQR();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler(), "/messages-spqr").setAllowedOrigins("*");
    }

    public static class ConversationGraph {
        @Autowired
        private ConversationRepository conversationRepository;

        @GraphQLQuery
        public Conversation getConversation(@GraphQLArgument(name = "id") @GraphQLNonNull Long conversationId) {
            return conversationRepository.findOne(conversationId);
        }

    }

    @Bean
    public ConversationGraph conversationGraph() {
        return new ConversationGraph();
    }

    public static class MessageGraph {

        @Autowired
        private MessageRepository messageRepository;

        @Autowired
        private MessageStreamer messagePublisher;

        @Autowired
        private MessageStreamer messageStreamer;

        @GraphQLQuery(name = "getAllMessages")
        public List<Message> getAllMessages() {
            return Lists.newArrayList(messageRepository.findAll());
        }

        @GraphQLMutation(name = "addMessage")
        public Message addMessage(
            @GraphQLArgument(name = "body") @GraphQLNonNull String body,
            @GraphQLArgument(name = "conversationId") @GraphQLNonNull Long conversationId,
            @GraphQLArgument(name = "carId") Long carId
        ) {
            return messageStreamer.emitMessage(new Message(null, body, conversationId, carId));
        }

        @GraphQLSubscription(name = "messages")
        public Publisher<Message> messages() {
            return messagePublisher.getPublisher();
        }
    }

    public static class CarGraph {

        private final JdbcTemplate jdbcTemplate;

        @Autowired
        public CarGraph(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        /*@GraphQLQuery(name = "car")
        public Car car(@GraphQLContext Message message *//* I can get Car in Message context *//*) {
            return jdbcTemplate.queryForObject("select * from car where id =" + message.getCarId(), (rs, rowNum) -> {
                Car car = new Car();
                car.setId(rs.getLong(1));
                car.setVin(rs.getString(2));
                return car;
            });
        }*/

        @GraphQLQuery(name = "car")
        @Batched
        public List<Car> cars(@GraphQLContext List<Message> messages) {
            return jdbcTemplate.query("select * from car where id in("
                + messages.stream().map(Message::getCarId).map(id -> id.toString()).collect(joining(","))
                + ")", (rs, rowNum) -> {
                Car car = new Car();
                car.setId(rs.getLong(1));
                car.setVin(rs.getString(2));
                return car;
            });
        }

        /*@GraphQLQuery(name = "car")
        @Batched
        public List<Car> cars(
            @GraphQLContext List<Message> messages, // I can get Car in Message context
            @GraphQLEnvironment Set<String> subFields
        ) {
            return http.getForObject(
                "http://localhost:9090/products?ids={id}",
                Cars.class,
                messages.stream().map(Message::getBody).collect(joining(",")),
                String.join(",", subFields)
            ).getCars();
            return jdbcTemplate.queryForList("select * from car where id in (" +
                messages.stream().map(Message::getCarId).map(id -> id.toString()).collect(joining(",")) +
                ")", Car.class);
        }*/

        @GraphQLQuery(name = "images")
        public List<String> images(
            @GraphQLContext Car car,
            @GraphQLArgument(name = "limit", defaultValue = "0") int limit
        ) {
            return car.getImages().subList(
                0, limit > 0 ? limit : car.getImages().size());
        }

    }
}

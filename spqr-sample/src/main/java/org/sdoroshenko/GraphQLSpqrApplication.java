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
import io.leangen.graphql.annotations.*;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableWebSocket
public class GraphQLSpqrApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLSpqrApplication.class, args);
    }

    @Bean
    public ConversationGraph conversationGraph() {
        return new ConversationGraph();
    }

    @Bean
    public MessageGraph messageGraph() {
        return new MessageGraph();
    }

    @Bean
    public CarGraph carGraph(NamedParameterJdbcTemplate jdbcTemplate) {
        return new CarGraph(jdbcTemplate);
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(conversationGraph())
                .withOperationsFromSingleton(messageGraph())
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

    public static class MessageGraph {

        @Autowired
        private MessageRepository messageRepository;

        @Autowired
        private MessageStreamer messagePublisher;

        @Autowired
        private MessageStreamer messageStreamer;

        @GraphQLQuery(name = "getAllMessages")
        public List<Message> getAllMessages(@GraphQLArgument(name = "limit", defaultValue = "0") int limit) {
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

        private final NamedParameterJdbcTemplate jdbcTemplate;

        @Autowired
        public CarGraph(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

       /* @GraphQLQuery(name = "car")
        public Car car(@GraphQLContext Message message) {
            return jdbcTemplate.queryForObject(
                    "select * from car where id = :carId",
                    new MapSqlParameterSource("carId", message.getCarId()),
                    (rs, rowNum) -> {
                        Car car = new Car();
                        car.setId(rs.getLong(1));
                        car.setVin(rs.getString(2));
                        return car;
                    }
            );
        }*/

        @GraphQLQuery(name = "car")
        @Batched
        public List<Car> cars(@GraphQLContext List<Message> messages) {
            List<Long> carIds = messages.stream().map(Message::getCarId).collect(Collectors.toList());
            SqlParameterSource parameters = new MapSqlParameterSource("carIds", carIds);

            return jdbcTemplate.query("select * from car where id in(:carIds)", parameters, (rs, rowNum) -> {
                Car car = new Car();
                car.setId(rs.getLong(1));
                car.setVin(rs.getString(2));
                return car;
            });
        }

        @GraphQLQuery(name = "images")
        public List<String> images(
            @GraphQLContext Car car,
            @GraphQLArgument(name = "limit", defaultValue = "0") int limit
        ) {
            return car.getImages().subList(
                0, limit > 0 ? limit : car.getImages().size());
        }
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
}

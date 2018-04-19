package org.sdoroshenko;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Lists;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.ExecutorServiceExecutionStrategy;
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
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Car;
import org.sdoroshenko.model.Conversation;
import org.sdoroshenko.model.Customer;
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
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    public CustomerGraph customerGraph() {
        return new CustomerGraph();
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());

        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(conversationGraph())
                .withOperationsFromSingleton(messageGraph())
                .withOperationsFromSingleton(carGraph)
                .withOperationsFromSingleton(customerGraph())
            .withTypeMappers(new JodaTypeAdapter()).withDefaultMappers()
                .withValueMapperFactory(new JacksonValueMapperFactory())
                .generate();

        ExecutorService executor = Executors.newCachedThreadPool();
        return GraphQL.newGraphQL(schema)
//                .queryExecutionStrategy(new BatchedExecutionStrategy())
//                .queryExecutionStrategy(new AsyncExecutionStrategy())
            .queryExecutionStrategy(new ExecutorServiceExecutionStrategy(executor))
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
            messageRepository.save(new Message(null, "one", conversation.getId(), car1.getId(), null, DateTime.now()));
            messageRepository.save(new Message(null, "two", conversation.getId(), car2.getId(), null, DateTime.now()));
        };
    }

    @Slf4j
    public static class CarGraph {

        private final NamedParameterJdbcTemplate jdbcTemplate;

        @Autowired
        public CarGraph(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @GraphQLQuery(name = "car")
        public Car car(@GraphQLContext Message message) {
            log.debug("{}. {} {} in: {}", 1, "start", "car", Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Car result = jdbcTemplate.queryForObject(
                    "select * from car where id = :carId",
                    new MapSqlParameterSource("carId", message.getCarId()),
                    (rs, rowNum) -> {
                        Car car = new Car();
                        car.setId(rs.getLong(1));
                        car.setVin(rs.getString(2));
                        return car;
                    }
            );
            log.debug("{}. {} {} in: {}", 2, "completed", "car", Thread.currentThread().getName());
            return result;
        }

        /*@GraphQLQuery(name = "car")
        @Batched
        public List<Car> cars(@GraphQLContext List<Message> messages) {
            log.debug(Thread.currentThread().getName() + ": executes 'car'");
            List<Long> carIds = messages.stream().map(Message::getCarId).collect(Collectors.toList());
            SqlParameterSource parameters = new MapSqlParameterSource("carIds", carIds);

            return jdbcTemplate.query("select * from car where id in(:carIds)", parameters, (rs, rowNum) -> {
                Car car = new Car();
                car.setId(rs.getLong(1));
                car.setVin(rs.getString(2));
                return car;
            });
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

    @Slf4j
    public static class CustomerGraph {

        @GraphQLQuery(name = "customer")
        public Customer findCustomer(@GraphQLContext Message message) {
            log.debug("{}. {} {} in: {}", 3, "start", "customer", Thread.currentThread().getName());
            Customer customer = new Customer(222L, "Test User");
            log.debug("{}. {} {} in: {}", 4, "completed", "customer", Thread.currentThread().getName());
            return customer;
        }

    }

    @Slf4j
    public static class MessageGraph {

        @Autowired
        private MessageRepository messageRepository;

        @Autowired
        private MessageStreamer messagePublisher;

        @Autowired
        private MessageStreamer messageStreamer;

        @GraphQLQuery(name = "getAllMessages")
        public List<Message> getAllMessages(@GraphQLArgument(name = "limit", defaultValue = "0") int limit) {
            log.debug("{}. {} {} in: {}", 0, "start", "messages", Thread.currentThread().getName());
            List<Message> messages = Lists.newArrayList(messageRepository.findAll());
            log.debug("{}. {} {} in: {}", 0, "completed", "messages", Thread.currentThread().getName());
            return messages;
        }

        @GraphQLMutation(name = "addMessage")
        public Message addMessage(
            @GraphQLArgument(name = "body") @GraphQLNonNull String body,
            @GraphQLArgument(name = "conversationId") @GraphQLNonNull Long conversationId,
            @GraphQLArgument(name = "carId") Long carId,
            @GraphQLArgument(name = "date") Long timestamp
        ) {
            return messageStreamer.emitMessage(new Message(null, body, conversationId, carId, null, new DateTime(timestamp)));
        }

        @GraphQLSubscription(name = "messages")
        public Publisher<Message> messages() {
            return messagePublisher.getPublisher();
        }
    }
}

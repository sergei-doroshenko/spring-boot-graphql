package org.sdoroshenko.spqr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.joda.time.DateTime;
import org.sdoroshenko.spqr.graph.CarGraph;
import org.sdoroshenko.spqr.graph.ConversationGraph;
import org.sdoroshenko.spqr.graph.CustomerGraph;
import org.sdoroshenko.spqr.graph.JodaTypeAdapter;
import org.sdoroshenko.spqr.graph.MessageGraph;
import org.sdoroshenko.spqr.model.Car;
import org.sdoroshenko.spqr.model.Conversation;
import org.sdoroshenko.spqr.model.Message;
import org.sdoroshenko.spqr.publisher.SocketHandlerSPQR;
import org.sdoroshenko.spqr.repository.CarRepository;
import org.sdoroshenko.spqr.repository.ConversationRepository;
import org.sdoroshenko.spqr.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableWebSocket
public class GraphQLSpqrApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLSpqrApplication.class, args);
    }

    @Bean
    public CarGraph carGraph(NamedParameterJdbcTemplate jdbcTemplate, DataLoaderRegistry registry) {
        return new CarGraph(jdbcTemplate, registry);
    }

    @Bean
    public CarService carService(NamedParameterJdbcTemplate jdbcTemplate) {
        return new CarService(jdbcTemplate);
    }

    @Bean
    public DataLoaderRegistry dataLoaderRegistry(CarService carService) {
        BatchLoader<Long, Car> carBatchLoader = ids -> {
            // we use supplyAsync() of values here for maximum parallelization
            return CompletableFuture.supplyAsync(() -> carService.getCarDataViaBatchSQL(ids));
        };
        DataLoader<Long, Car> carDataLoader = DataLoader.newDataLoader(carBatchLoader);
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("car", carDataLoader);
        return registry;
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph, DataLoaderRegistry registry) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());

        GraphQLSchema schema = new GraphQLSchemaGenerator()
            .withOperationsFromSingletons(conversationGraph(), messageGraph(), customerGraph(), carGraph)
            .withTypeMappers((config, defaults) -> defaults.insert(0, new JodaTypeAdapter()))
            .withValueMapperFactory(new JacksonValueMapperFactory())
            .generate();


        DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(registry);

        ExecutorService executor = Executors.newCachedThreadPool();
        return GraphQL.newGraphQL(schema)
            // deprecated
//            .queryExecutionStrategy(new BatchedExecutionStrategy())
            .queryExecutionStrategy(new AsyncExecutionStrategy())
//            .queryExecutionStrategy(new ExecutorServiceExecutionStrategy(executor))
            .instrumentation(new ChainedInstrumentation(Arrays.asList(
                new MaxQueryComplexityInstrumentation(200),
                new MaxQueryDepthInstrumentation(20),
                dispatcherInstrumentation
            )))
            .build();
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
    public CustomerGraph customerGraph() {
        return new CustomerGraph();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler(), "/messages-spqr/*").setAllowedOrigins("*");
    }

    @Bean
    public SocketHandlerSPQR socketHandler() {
        return new SocketHandlerSPQR();
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
            messageRepository.save(new Message(null, "one", conversation.getId(), car1.getId(), null, DateTime.now(), null));
            messageRepository.save(new Message(null, "two", conversation.getId(), car2.getId(), null, DateTime.now(), null));
        };
    }
}

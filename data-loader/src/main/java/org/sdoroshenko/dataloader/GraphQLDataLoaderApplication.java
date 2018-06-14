package org.sdoroshenko.dataloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.joda.time.DateTime;
import org.sdoroshenko.dataloader.graph.CarGraph;
import org.sdoroshenko.dataloader.graph.JodaTypeAdapter;
import org.sdoroshenko.dataloader.graph.MessageGraph;
import org.sdoroshenko.dataloader.model.Car;
import org.sdoroshenko.dataloader.model.Conversation;
import org.sdoroshenko.dataloader.model.Message;
import org.sdoroshenko.dataloader.repository.CarRepository;
import org.sdoroshenko.dataloader.repository.ConversationRepository;
import org.sdoroshenko.dataloader.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class GraphQLDataLoaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLDataLoaderApplication.class, args);
    }

    @Bean
    public CarService carService(NamedParameterJdbcTemplate jdbcTemplate) {
        return new CarService(jdbcTemplate);
    }

    @Bean
    public DataLoaderRegistry dataLoaderRegistry(CarService carService) {
        BatchLoader<Long, Car> characterBatchLoader = ids -> {
            // we use supplyAsync() of values here for maximum parallelization
            return CompletableFuture.supplyAsync(() -> carService.getCarDataViaBatchSQL(ids));
        };
        DataLoader<Long, Car> carDataLoader = new DataLoader<>(characterBatchLoader);
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("car", carDataLoader);
        return registry;
    }

    @Bean
    public CarGraph carGraph(DataLoaderRegistry registry) {
        return new CarGraph(registry);
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph, DataLoaderRegistry registry) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());

        GraphQLSchema schema = new GraphQLSchemaGenerator()
            .withOperationsFromSingletons(messageGraph(), carGraph)
            .withTypeMappers((config, defaults) -> defaults.insert(0, new JodaTypeAdapter()))
            .withValueMapperFactory(new JacksonValueMapperFactory())
            .generate();

        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(new AsyncExecutionStrategy())
            .instrumentation(
                new DataLoaderDispatcherInstrumentation(registry) // need to dispatch query execution to particular DataLoader
            )
            .build();
    }

    @Bean
    public MessageGraph messageGraph() {
        return new MessageGraph();
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
            messageRepository.save(new Message(null, "one", conversation.getId(), car1.getId(), DateTime.now()));
            messageRepository.save(new Message(null, "two", conversation.getId(), car2.getId(), DateTime.now()));
        };
    }
}

package org.sdoroshenko.config;

import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.sdoroshenko.publisher.MessagePublisher;
import org.sdoroshenko.repository.CarRepository;
import org.sdoroshenko.spqr.CarGraph;
import org.sdoroshenko.spqr.Messenger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Arrays;

@Configuration
public class SpqrConfig extends WebMvcConfigurerAdapter {

    @Bean
    public CarGraph carGraph(CarRepository carRepository) {
        return new CarGraph(carRepository);
    }

    @Bean
    public Messenger messageSubscription(MessagePublisher messagePublisher) {
        return new Messenger(messagePublisher);
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph, Messenger messageSubscription) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
            .withResolverBuilders(
                new BeanResolverBuilder("org.sdoroshenko.model"),
                // Resolve by annotations.
                new AnnotatedResolverBuilder(),
                // Resolve public methods inside root package.
                new PublicResolverBuilder("org.sdoroshenko.spqr"))
            .withOperationsFromSingleton(carGraph)
            .withOperationsFromSingleton(messageSubscription)
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
}

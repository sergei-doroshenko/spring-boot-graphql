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
import org.sdoroshenko.repository.CarRepository;
import org.sdoroshenko.spqr.CarGraph;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@ConditionalOnProperty(name = "graphql.type", havingValue = "spqr")
public class SpqrConfig {

    @Bean
    public CarGraph carGraph(CarRepository carRepository) {
        return new CarGraph(carRepository);
    }

    @Bean
    public GraphQL graphQL(CarGraph carGraph) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
            .withResolverBuilders(
                new BeanResolverBuilder("org.sdoroshenko.model"),
                // Resolve by annotations.
                new AnnotatedResolverBuilder(),
                // Resolve public methods inside root package.
                new PublicResolverBuilder("org.sdoroshenko.spqr"))
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
}

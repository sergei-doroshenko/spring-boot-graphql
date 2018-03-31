package org.sdoroshenko.publisher;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class MessageGraphqlPublisher {
    private static final Logger logger = LoggerFactory.getLogger(MessageGraphqlPublisher.class);
    private static final MessageStreamer messagePublisher = new MessageStreamer();

    @Getter
    private final GraphQLSchema graphQLSchema;

    public MessageGraphqlPublisher() {
        graphQLSchema = buildSchema();
    }

    private GraphQLSchema buildSchema() {
        Reader streamReader = null;
        try {
            streamReader = loadSchemaFile("graphql/car.graphqls");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Subscription")
                        .dataFetcher("messages", messagesSubscriptionFetcher())
                )
                .build();

        return new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
    }

    /**
     * The DataFetcher behind a subscription field is responsible for creating the Publisher of data.
     * @return {@link DataFetcher}
     */
    private DataFetcher messagesSubscriptionFetcher() {
        return environment -> messagePublisher.getPublisher();
    }

    @SuppressWarnings("SameParameterValue")
    private Reader loadSchemaFile(String name) throws UnsupportedEncodingException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    }

}

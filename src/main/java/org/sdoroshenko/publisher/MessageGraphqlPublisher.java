package org.sdoroshenko.publisher;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class MessageGraphqlPublisher {
    private final static MessagePublisher MESSAGE_PUBLISHER = new MessagePublisher();

    private final GraphQLSchema graphQLSchema;

    public MessageGraphqlPublisher() {
        graphQLSchema = buildSchema();
    }

    private GraphQLSchema buildSchema() {
        //
        // reads a file that provides the schema types
        //
        Reader streamReader = null;
        try {
            streamReader = loadSchemaFile("graphql/car.graphqls");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
     * @return
     */
    private DataFetcher messagesSubscriptionFetcher() {
        return environment -> {
            List<String> arg = environment.getArgument("fields");
            List<String> messageFilter = arg == null ? Collections.emptyList() : arg;
            if (messageFilter.isEmpty()) {
                return MESSAGE_PUBLISHER.getPublisher();
            } else {
                return MESSAGE_PUBLISHER.getPublisher(messageFilter);
            }
        };
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    @SuppressWarnings("SameParameterValue")
    private Reader loadSchemaFile(String name) throws UnsupportedEncodingException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    }

}

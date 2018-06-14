package org.sdoroshenko.deferred;

import graphql.Directives;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@SpringBootApplication
@EnableWebSocket
public class GraphQLDeferredApp implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLDeferredApp.class, args);
    }

    @Bean
    public GraphQL graphQL(MessageRepository messageRepository) {
        //
        // reads a file that provides the schema types
        //
        Reader streamReader = loadSchemaFile("messages.graphqls");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("messages", environment -> messageRepository.findAll())
                )
                .build();

        GraphQLSchema schema = new SchemaGenerator()
                .makeExecutableSchema(typeRegistry, wiring)
                .transform(builder ->
                        builder.additionalDirective(Directives.DeferDirective)
                );

        return GraphQL.newGraphQL(schema).build();
    }

    private Reader loadSchemaFile(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        return new InputStreamReader(stream);
    }

    @Bean
    public GraphQLSocketHandler socketHandler() {
        return new GraphQLSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler(), "/qraphql").setAllowedOrigins("*");
    }

    @Bean
    public CommandLineRunner startup(MessageRepository messageRepository) {
        return (args) -> {
            messageRepository.save(new Message(null, "test01"));
            messageRepository.save(new Message(null, "test02"));
            messageRepository.save(new Message(null, "test03"));
            messageRepository.save(new Message(null, "test04"));
        };
    }

}

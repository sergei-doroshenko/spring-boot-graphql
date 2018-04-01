package org.sdoroshenko;

import com.google.common.collect.Lists;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Conversation;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.publisher.MessageStreamer;
import org.sdoroshenko.publisher.SocketHandlerSPQR;
import org.sdoroshenko.repository.ConversationRepository;
import org.sdoroshenko.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableWebSocket
public class GraphQLSpqrApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLSpqrApplication.class, args);
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
                @GraphQLArgument(name = "conversationId") @GraphQLNonNull Long conversationId
        ) {
            return messageStreamer.emitMessage(new Message(null, body, conversationId));
        }

        @GraphQLSubscription(name = "messages")
        public Publisher<Message> messages() {
            return messagePublisher.getPublisher();
        }
    }

    @Bean
    public MessageGraph messageGraph() {
        return new MessageGraph();
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

    @Bean
    public GraphQL graphQL(MessageGraph messageGraph, ConversationGraph conversationGraph) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(messageGraph)
                .withOperationsFromSingleton(conversationGraph)
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
    public CommandLineRunner startup(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        return (args) -> {
            Conversation conversation = new Conversation();
            conversationRepository.save(conversation);
            messageRepository.save(new Message(null, "one", conversation.getId()));
            messageRepository.save(new Message(null, "two", conversation.getId()));
        };
    }
}

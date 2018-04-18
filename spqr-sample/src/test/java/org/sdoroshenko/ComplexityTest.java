package org.sdoroshenko;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;
import org.junit.Test;
import org.sdoroshenko.model.Conversation;
import org.sdoroshenko.model.Customer;
import org.sdoroshenko.model.Message;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ComplexityTest {

    String operation5 = "{conversation(id: 1) {\n" +
            "    id\n" +
            "    messages {\n" +
            "      id\n" +
            "      body\n" +
            "}}}"; // has total complexity of 5

    GraphQLSchema schema = new GraphQLSchemaGenerator()
            .withBasePackages("org.sdoroshenko")
            .withOperationsFromSingleton(new ConversationService())
            .generate();

    @Test
    public void testComplexity() {
        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(5)
                .build();

        ExecutionResult res = exe.execute(operation5);
        assertEquals(0, res.getErrors().size());
    }

    @Test(expected = ComplexityLimitExceededException.class)
    public void testComplexityExceeded() {
        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(4)
                .build();

        ExecutionResult res = exe.execute(operation5);
        assertEquals(1, res.getErrors().size());
    }

    public static class ConversationService {

        @GraphQLQuery(name = "conversation")
//        @GraphQLComplexity("2 + childScore")
        public Conversation getConversation(@GraphQLArgument(name = "id") @GraphQLNonNull Long conversationId) {
            Conversation conversation = new Conversation();
            conversation.setId(100L);
            conversation.setMessages(
                    Arrays.asList(
                            new Message(20L, "test 1", conversation.getId(), null, new Customer()),
                            new Message(30L, "test 2", conversation.getId(), null, new Customer())
                    )
            );

            return conversation;
        }

    }
}

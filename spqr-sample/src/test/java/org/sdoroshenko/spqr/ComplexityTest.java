package org.sdoroshenko.spqr;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.complexity.ComplexityLimitExceededException;
import org.joda.time.DateTime;
import org.junit.Test;
import org.sdoroshenko.spqr.model.Conversation;
import org.sdoroshenko.spqr.model.Customer;
import org.sdoroshenko.spqr.model.Message;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ComplexityTest {

    String operation5 = "{conversation(id: 1) {\n" +
            "    id\n" +
            "    messages {\n" +
            "      id\n" +
            "      body\n" +
            "}}}"; // has total complexity of 5

    GraphQLSchema schema = new GraphQLSchemaGenerator()
        .withBasePackages("org.sdoroshenko.spqr")
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

    @Test
    public void testComplexityExceeded() {
        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
            .maximumQueryComplexity(2)
                .build();

        ExecutionResult res = exe.execute(operation5);
        assertEquals(1, res.getErrors().size());
        assertTrue(res.getErrors().get(0).getClass().isAssignableFrom(ComplexityLimitExceededException.class));
    }

    public static class ConversationService {

        @GraphQLQuery(name = "conversation")
//        @GraphQLComplexity("2 + childScore")
        public Conversation getConversation(@GraphQLArgument(name = "id") @GraphQLNonNull Long conversationId) {
            Conversation conversation = new Conversation();
            conversation.setId(100L);
            conversation.setMessages(
                    Arrays.asList(
                        new Message(20L, "test 1", conversation.getId(), null, new Customer(), DateTime.now(), null),
                        new Message(30L, "test 2", conversation.getId(), null, new Customer(), DateTime.now(), null)
                    )
            );

            return conversation;
        }

    }
}

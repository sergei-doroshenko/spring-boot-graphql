package org.sdoroshenko;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.junit.Test;
import org.sdoroshenko.model.Conversation;
import org.sdoroshenko.model.Message;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class QueryTest {

    String query = "query ($param: Long!) {\n" +
            "  conversation(id: $param) {\n" +
            "    id\n" +
            "    messages {\n" +
            "      id\n" +
            "      body\n" +
            "}}}";

    Map<String, Object> params = ImmutableMap.of("param", 1);

    GraphQLSchema schema = new GraphQLSchemaGenerator()
            .withBasePackages("org.sdoroshenko")
            .withOperationsFromSingleton(new ConversationService())
            .generate();

    @Test
    public void testVariables() {
        GraphQL exe = GraphQLRuntime.newGraphQL(schema)
                .maximumQueryComplexity(50)
                .build();

        ExecutionResult res = exe.execute(
                ExecutionInput.newExecutionInput()
                .query(query)
                .variables(params)
                .build()
        );
        assertEquals(0, res.getErrors().size());
    }

    public static class ConversationService {

        @GraphQLQuery(name = "conversation")
        public Conversation getConversation(@GraphQLArgument(name = "id") @GraphQLNonNull Long conversationId) {
            Conversation conversation = new Conversation();
            conversation.setId(100L);
            conversation.setMessages(
                    Arrays.asList(
                            new Message(20L, "test 1", conversation.getId(), null, null, null),
                            new Message(30L, "test 2", conversation.getId(), null, null, null)
                    )
            );

            return conversation;
        }

    }
}

package org.sdoroshenko.spqr.graph;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.extern.slf4j.Slf4j;
import org.sdoroshenko.spqr.model.Conversation;
import org.sdoroshenko.spqr.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class ConversationGraph {

    @Autowired
    private ConversationRepository conversationRepository;

    @GraphQLQuery
    public Conversation getConversation(@GraphQLArgument(name = "id") @GraphQLNonNull Long conversationId,
                                        @GraphQLArgument(name = "filters") List<Filter> filters) {
        log.debug(filters.toString());
        return conversationRepository.findOne(conversationId);
    }

    @SuppressWarnings("unused")
    @GraphQLType(description = "Conversation filters")
    enum Filter {
        @GraphQLEnumValue(deprecationReason = "Impossible") ACTIVE,
        @GraphQLEnumValue(name = "smart") SMART_DEALER,
        @GraphQLEnumValue(name = "light") LIGHT_DEALER
    }
}

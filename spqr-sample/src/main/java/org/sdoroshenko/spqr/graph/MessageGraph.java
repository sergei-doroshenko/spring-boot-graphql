package org.sdoroshenko.spqr.graph;

import com.google.common.collect.Lists;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.sdoroshenko.spqr.dto.Filters;
import org.sdoroshenko.spqr.dto.MessageDto;
import org.sdoroshenko.spqr.model.Message;
import org.sdoroshenko.spqr.publisher.MessageStreamer;
import org.sdoroshenko.spqr.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class MessageGraph {

    final String defaultFilters = "{\"bodyFilter\": \"defaultBody\", \"authorFilter\": \"defaultAuthor\"}";
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageStreamer messagePublisher;
    @Autowired
    private MessageStreamer messageStreamer;

    @GraphQLQuery(name = "getAllMessages", description = "Fetch all messages")
    public List<Message> getAllMessages(
        @GraphQLArgument(name = "limit", defaultValue = "0") int limit,
        @GraphQLArgument(name = "filters", defaultValue = defaultFilters) Filters filters) {
        log.debug("Filters {}", filters);
        log.debug("{}. {} {} in: {}", 0, "start", "messages", Thread.currentThread().getName());
        List<Message> messages = Lists.newArrayList(messageRepository.findAll());
        log.debug("{}. {} {} in: {}", 0, "completed", "messages", Thread.currentThread().getName());
        return messages;
    }

    @GraphQLMutation(name = "addMessage", description = "Adds a new message to conversation")
    public Message addMessage(
        @GraphQLArgument(name = "body") @GraphQLNonNull String body,
        @GraphQLArgument(name = "conversationId") @GraphQLNonNull Long conversationId,
        @GraphQLArgument(name = "carId") Long carId,
        @GraphQLArgument(name = "date") Long timestamp
    ) {
        return messageStreamer.emitMessage(new Message(null, body, conversationId, carId, null, new DateTime(timestamp), null));
    }

    @GraphQLMutation(name = "addMessageViaInput")
    public Message addMessage2(@GraphQLArgument(name = "input") @GraphQLNonNull MessageDto messageDto) {
        return messageStreamer.emitMessage(
            new Message(
                null,
                messageDto.body,
                messageDto.conversationId,
                messageDto.carId,
                messageDto.customer,
                new DateTime(messageDto.createdAt),
                null
            )
        );
    }

    @GraphQLSubscription(name = "messages")
    public Publisher<Message> messages() {
        return messagePublisher.getPublisher();
    }
}

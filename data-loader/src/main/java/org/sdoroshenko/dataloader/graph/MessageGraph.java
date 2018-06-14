package org.sdoroshenko.dataloader.graph;

import com.google.common.collect.Lists;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.sdoroshenko.dataloader.model.Message;
import org.sdoroshenko.dataloader.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class MessageGraph {

    @Autowired
    private MessageRepository messageRepository;

    @GraphQLQuery(name = "getAllMessages", description = "Fetch all messages")
    public List<Message> getAllMessages(
        @GraphQLArgument(name = "limit", defaultValue = "0") int limit) {
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
        return messageRepository.save(new Message(null, body, conversationId, carId, new DateTime(timestamp)));
    }
}

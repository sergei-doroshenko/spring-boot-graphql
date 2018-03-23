package org.sdoroshenko.spqr;

import io.leangen.graphql.annotations.GraphQLSubscription;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.publisher.MessageStreamer;
import org.springframework.beans.factory.annotation.Autowired;

public class MessageGraph {

    @Autowired
    private MessageStreamer messagePublisher;

    @GraphQLSubscription(name = "messages")
    public Publisher<Message> messages() {
        return messagePublisher.getPublisher();
    }
}

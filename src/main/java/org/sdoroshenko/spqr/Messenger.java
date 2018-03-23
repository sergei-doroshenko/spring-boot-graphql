package org.sdoroshenko.spqr;

import io.leangen.graphql.annotations.GraphQLSubscription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.publisher.MessagePublisher;

@AllArgsConstructor
public class Messenger {

    @Getter @Setter
    private MessagePublisher messagePublisher;

    @GraphQLSubscription(name = "messages")
    public Publisher<Message> messages() {
        return messagePublisher.getPublisher();
    }
}

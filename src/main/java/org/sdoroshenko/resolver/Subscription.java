package org.sdoroshenko.resolver;

import com.coxautodev.graphql.tools.GraphQLSubscriptionResolver;
import org.sdoroshenko.model.Message;

public class Subscription implements GraphQLSubscriptionResolver {

    public Message messages() {
        return new Message(2L, "test");
    }

}

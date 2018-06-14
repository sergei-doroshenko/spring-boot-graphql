package org.sdoroshenko.spqr.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.sdoroshenko.spqr.GraphQLRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor
public class SocketHandlerSPQR extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SocketHandlerSPQR.class);

    @Getter @Setter @Autowired private GraphQL graphQL;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connected session: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String graphqlQuery = message.getPayload();
        logger.info("Server got {}", graphqlQuery);

        ObjectMapper mapper = new ObjectMapper();
        GraphQLRequest graphQLRequest = mapper.readValue(graphqlQuery, GraphQLRequest.class);

        ExecutionResult res = graphQL.execute(graphQLRequest.getQuery());
        Publisher<ExecutionResult> stream = res.getData();

        stream.subscribe(new Subscriber<ExecutionResult>() {
            private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

            @Override
            public void onSubscribe(Subscription subscription) {
                subscriptionRef.set(subscription);
                request(1);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                logger.debug("Sending message update");
                processMessage(executionResult, session);
                request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Subscription threw an exception", throwable);
                closeSession(session);
            }

            @Override
            public void onComplete() {
                logger.info("Subscription complete");
                closeSession(session);
            }

            private void request(int n) {
                Subscription subscription = subscriptionRef.get();
                if (subscription != null) {
                    subscription.request(n);
                }
            }
        });
    }

    private void processMessage(ExecutionResult executionResult, WebSocketSession session) {
        Object messageUpdate = executionResult.getData();
        try {
            session.sendMessage(new TextMessage(messageUpdate.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

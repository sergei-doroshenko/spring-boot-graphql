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
import org.sdoroshenko.spqr.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
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

        Subscriber<ExecutionResult> subscriber = new MessageSubscriber(session);
        stream.subscribe(subscriber);
    }

    /**
     * For each connection created a {@link Subscriber}.
     */
    public static class MessageSubscriber implements Subscriber<ExecutionResult> {
        private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
        private WebSocketSession session;
        private Long conversationId;
        private ObjectMapper mapper = new ObjectMapper();

        public MessageSubscriber(WebSocketSession session) {
            this.session = session;
            // get param from connection uri
            String path = session.getUri().getPath();
            String conversationIdParam = path.substring(path.lastIndexOf('/') + 1);
            this.conversationId = Long.valueOf(conversationIdParam);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscriptionRef.set(subscription);
            request(1);
        }

        /**
         * Based on {@link ExecutionResult} decide weather to send a WebSocket notification or not.
         *
         * @param executionResult {@link ExecutionResult}
         * @seee {@link #processMessage(ExecutionResult)}
         */
        @Override
        public void onNext(ExecutionResult executionResult) {
            logger.debug("Sending message update");
            processMessage(executionResult);
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

        private void closeSession(WebSocketSession session) {
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Here we extract {@link Message} from {@link ExecutionResult} and check {@link Message#conversationId}.
         *
         * @param executionResult
         */
        private void processMessage(ExecutionResult executionResult) {
            try {
                // extract
                Map<String, Map<String, Message>> messageUpdate = executionResult.getData();
                String json = mapper.writeValueAsString(messageUpdate.get("messages"));
                Message message = mapper.readValue(json, Message.class);
                // check
                if (this.conversationId.equals(message.getConversationId())) {
                    // notify - send message
                    session.sendMessage(new TextMessage(json));
                } else {
                    // notify - send some predefined
                    session.sendMessage(new TextMessage("PING"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void request(int n) {
            Subscription subscription = subscriptionRef.get();
            if (subscription != null) {
                subscription.request(n);
            }
        }
    }
}

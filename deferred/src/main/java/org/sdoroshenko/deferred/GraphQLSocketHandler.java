package org.sdoroshenko.deferred;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GraphQLSocketHandler extends TextWebSocketHandler {

    @Autowired
    private GraphQL graphQL;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected session: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String graphqlQuery = message.getPayload();
        log.info("Server got {}", graphqlQuery);

        ObjectMapper mapper = new ObjectMapper();
        GraphQLRequest graphQLRequest = mapper.readValue(graphqlQuery, GraphQLRequest.class);
        String deferredQuery = graphQLRequest.getQuery();

        //
        // deferredQuery contains the query with @defer directives in it
        //
        ExecutionResult initialResult = graphQL.execute(ExecutionInput.newExecutionInput().query(deferredQuery).build());

        //
        // then initial results happen first, the deferred ones will begin AFTER these initial
        // results have completed
        //
        sendResult(session, initialResult);

        Map<Object, Object> extensions = initialResult.getExtensions();
        Publisher<ExecutionResult> deferredResults = (Publisher<ExecutionResult>) extensions.get(GraphQL.DEFERRED_RESULTS);

        //
        // you subscribe to the deferred results like any other reactive stream
        //
        deferredResults.subscribe(new Subscriber<ExecutionResult>() {

            Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                //
                // how many you request is up to you
                subscription.request(1);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                //
                // as each deferred result arrives, send it to where it needs to go
                //
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendResult(session, executionResult);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                handleError(session, t);
            }

            @Override
            public void onComplete() {
                completeResponse(session);
            }
        });
    }
    private void sendResult(WebSocketSession session, ExecutionResult executionResult) {
        Object messageUpdate = executionResult.getData();
        try {
            session.sendMessage(new TextMessage(messageUpdate.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleError(WebSocketSession session, Throwable t) {

    }

    private void completeResponse(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package org.sdoroshenko;

import org.sdoroshenko.publisher.MessageGraphqlPublisher;
import org.sdoroshenko.resolver.SocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler(sessionStorage(), messageGraphqlPublisher()), "/messages").setAllowedOrigins("*");
    }

    @Bean
    public List<WebSocketSession> sessionStorage() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    MessageGraphqlPublisher messageGraphqlPublisher() {
        return new MessageGraphqlPublisher();
    }

    @Bean
    public SocketHandler socketHandler(List<WebSocketSession> sessionStorage, MessageGraphqlPublisher messageGraphqlPublisher) {
        return new SocketHandler(sessionStorage, messageGraphqlPublisher);
    }
}

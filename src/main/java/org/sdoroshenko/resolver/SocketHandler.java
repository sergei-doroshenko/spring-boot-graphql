package org.sdoroshenko.resolver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class SocketHandler extends TextWebSocketHandler {

    @Getter @Setter private List<WebSocketSession> sessions;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        for(WebSocketSession webSocketSession : sessions) {
            webSocketSession.sendMessage(new TextMessage("Server got: " + message.getPayload() + " !"));
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //the messages will be broadcasted to all users.
        sessions.add(session);
    }
}

package com.example.aiwerewolf.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketPushService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketPushService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushPublicEvent(@NonNull String roomId, @NonNull Object payload) {
        safeSend("/topic/rooms/" + roomId + "/public", payload);
    }

    public void pushPrivateEventToPlayer(@NonNull String username, @NonNull String roomId, @NonNull Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/rooms/" + roomId + "/private", payload);
        } catch (RuntimeException ex) {
            log.warn("WebSocket private push failed: {}", ex.getMessage());
        }
    }

    public void pushGodViewEventToObservers(@NonNull String roomId, @NonNull Object payload) {
        safeSend("/topic/rooms/" + roomId + "/god-view", payload);
    }

    public void pushPhaseChanged(@NonNull String roomId, @NonNull Object payload) {
        safeSend("/topic/rooms/" + roomId + "/phase", payload);
    }

    public void pushTimelineUpdated(@NonNull String roomId, @NonNull Object payload) {
        safeSend("/topic/rooms/" + roomId + "/timeline", payload);
    }

    private void safeSend(@NonNull String destination, @NonNull Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (RuntimeException ex) {
            log.warn("WebSocket push failed: {}", ex.getMessage());
        }
    }
}

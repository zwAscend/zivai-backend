package zw.co.zivai.core_backend.websockets;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatSocketRegistry {
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(String studentId, WebSocketSession session) {
        sessions.computeIfAbsent(studentId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String studentId, WebSocketSession session) {
        if (studentId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(studentId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(studentId);
        }
    }

    public void broadcast(String studentId, Object payload) {
        if (studentId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(studentId);
        if (set == null || set.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            set.removeIf(session -> !session.isOpen());
            for (WebSocketSession session : set) {
                try {
                    session.sendMessage(message);
                } catch (IOException ex) {
                    // Drop dead sessions; next broadcast will clean them up.
                }
            }
        } catch (Exception ex) {
            // No-op: keep realtime optional.
        }
    }
}

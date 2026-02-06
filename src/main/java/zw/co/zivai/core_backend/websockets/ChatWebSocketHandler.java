package zw.co.zivai.core_backend.websockets;

import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final String STUDENT_ID_ATTR = "studentId";
    private final ChatSocketRegistry socketRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String studentId = extractStudentId(session.getUri());
        if (studentId == null || studentId.isBlank()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put(STUDENT_ID_ATTR, studentId);
        socketRegistry.register(studentId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object value = session.getAttributes().get(STUDENT_ID_ATTR);
        socketRegistry.unregister(value != null ? value.toString() : null, session);
    }

    private String extractStudentId(URI uri) {
        if (uri == null) {
            return null;
        }
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        return params.getFirst("studentId");
    }
}

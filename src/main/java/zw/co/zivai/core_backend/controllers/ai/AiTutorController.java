package zw.co.zivai.core_backend.controllers.ai;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.ai.AiTutorMessageDto;
import zw.co.zivai.core_backend.dtos.ai.AiTutorSessionDto;
import zw.co.zivai.core_backend.dtos.ai.CreateAiTutorMessageRequest;
import zw.co.zivai.core_backend.dtos.ai.CreateAiTutorSessionRequest;
import zw.co.zivai.core_backend.services.ai.AiTutorService;

@RestController
@RequestMapping("/api/ai-tutor")
@RequiredArgsConstructor
public class AiTutorController {
    private final AiTutorService aiTutorService;

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public AiTutorSessionDto createSession(@RequestBody CreateAiTutorSessionRequest request) {
        return aiTutorService.getOrCreateSession(request);
    }

    @GetMapping("/sessions")
    public List<AiTutorSessionDto> listSessions(@RequestParam UUID studentId) {
        return aiTutorService.listSessions(studentId);
    }

    @GetMapping("/messages")
    public List<AiTutorMessageDto> listMessages(@RequestParam UUID sessionId) {
        return aiTutorService.listMessages(sessionId);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public AiTutorMessageDto createMessage(@RequestBody CreateAiTutorMessageRequest request) {
        return aiTutorService.createMessage(request);
    }
}

package zw.co.zivai.core_backend.controllers.termforecast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.termforecast.TermForecastRequest;
import zw.co.zivai.core_backend.dtos.termforecast.TermForecastResponse;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.TermForecast;
import zw.co.zivai.core_backend.repositories.termforecast.TermForecastRepository;
import zw.co.zivai.core_backend.services.termforecast.TermForecastService;

@RestController
@RequestMapping("/api/term-forecasts")
@RequiredArgsConstructor
public class TermForecastController {
    private final TermForecastService termForecastService;
    private final ObjectMapper objectMapper;
    private final TermForecastRepository termForecastRepository;

    @GetMapping
    public List<TermForecastResponse> list(
        @RequestParam UUID subjectId,
        @RequestParam(required = false) String term
    ) {
        return termForecastService.listBySubject(subjectId, term).stream()
            .map(this::toResponse)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermForecastResponse create(@RequestBody TermForecastRequest request) {
        return toResponse(termForecastService.create(request));
    }

    @PutMapping("/{id}")
    public TermForecastResponse update(@PathVariable UUID id, @RequestBody TermForecastRequest request) {
        return toResponse(termForecastService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        termForecastService.delete(id);
    }

    private TermForecastResponse toResponse(TermForecast forecast) {
        ClassSubject classSubject = forecast.getClassSubject();
        Subject subject = classSubject != null ? classSubject.getSubject() : null;
        ClassEntity classEntity = classSubject != null ? classSubject.getClassEntity() : null;
        List<String> expectedTopicIds = extractExpectedTopicIds(forecast.getExpectedTopicIds());
        if (expectedTopicIds.isEmpty()) {
            String rawExpected = termForecastRepository.findExpectedTopicIdsTextById(forecast.getId());
            expectedTopicIds = extractExpectedTopicIdsFromText(rawExpected);
        }
        return TermForecastResponse.builder()
            .id(forecast.getId())
            .term(forecast.getTerm())
            .academicYear(forecast.getAcademicYear())
            .expectedCoveragePct(forecast.getExpectedCoveragePct())
            .expectedTopicIds(expectedTopicIds)
            .notes(forecast.getNotes())
            .classSubject(
                classSubject == null ? null : TermForecastResponse.ClassSubjectSnapshot.builder()
                    .id(classSubject.getId())
                    .subject(subject == null ? null : TermForecastResponse.SubjectSnapshot.builder()
                        .id(subject.getId())
                        .name(subject.getName())
                        .build())
                    .classEntity(classEntity == null ? null : TermForecastResponse.ClassSnapshot.builder()
                        .id(classEntity.getId())
                        .name(classEntity.getName())
                        .build())
                    .build()
            )
            .build();
    }

    private List<String> extractExpectedTopicIds(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> ids.add(item.asText()));
            return ids;
        }
        if (node.isTextual()) {
            return parseExpectedTopicText(node.asText());
        }
        try {
            List<String> converted = objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            if (converted != null) {
                ids.addAll(converted);
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        if (!ids.isEmpty()) {
            return ids;
        }
        return extractExpectedTopicIdsFromText(node.toString());
    }

    private List<String> extractExpectedTopicIdsFromText(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String candidate = raw;
        for (int i = 0; i < 2; i += 1) {
            try {
                JsonNode parsed = objectMapper.readTree(candidate);
                if (parsed != null && parsed.isArray()) {
                    List<String> ids = new ArrayList<>();
                    parsed.forEach(item -> ids.add(item.asText()));
                    return ids;
                }
                if (parsed != null && parsed.isTextual()) {
                    candidate = parsed.asText();
                    continue;
                }
            } catch (Exception ignored) {
                break;
            }
        }
        return List.of();
    }

    private List<String> parseExpectedTopicText(String raw) {
        return extractExpectedTopicIdsFromText(raw);
    }
}

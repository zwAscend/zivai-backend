package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.TermForecastRequest;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.TermForecast;
import zw.co.zivai.core_backend.repositories.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.TermForecastRepository;

@Service
@RequiredArgsConstructor
public class TermForecastService {
    private final TermForecastRepository termForecastRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ObjectMapper objectMapper;

    public TermForecast create(TermForecastRequest request) {
        ClassSubject classSubject = resolveClassSubject(request);

        TermForecast forecast = new TermForecast();
        forecast.setClassSubject(classSubject);
        forecast.setTerm(request.getTerm());
        forecast.setAcademicYear(request.getAcademicYear());
        forecast.setExpectedCoveragePct(normalizeCoverage(request.getExpectedCoveragePercent()));
        forecast.setExpectedTopicIds(toJson(request.getExpectedTopicIds()));
        forecast.setNotes(request.getNotes());
        return termForecastRepository.save(forecast);
    }

    public TermForecast update(UUID id, TermForecastRequest request) {
        TermForecast forecast = termForecastRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("Term forecast not found"));

        if (request.getClassSubjectId() != null || request.getSubjectId() != null) {
            ClassSubject classSubject = resolveClassSubject(request);
            forecast.setClassSubject(classSubject);
        }
        if (request.getTerm() != null) {
            forecast.setTerm(request.getTerm());
        }
        if (request.getAcademicYear() != null) {
            forecast.setAcademicYear(request.getAcademicYear());
        }
        if (request.getExpectedCoveragePercent() != null) {
            forecast.setExpectedCoveragePct(normalizeCoverage(request.getExpectedCoveragePercent()));
        }
        if (request.getExpectedTopicIds() != null) {
            forecast.setExpectedTopicIds(toJson(request.getExpectedTopicIds()));
        }
        if (request.getNotes() != null) {
            forecast.setNotes(request.getNotes());
        }
        return termForecastRepository.save(forecast);
    }

    public List<TermForecast> listBySubject(UUID subjectId, String term) {
        return termForecastRepository.findLatestBySubjectAndTerm(subjectId, term);
    }

    public void delete(UUID id) {
        TermForecast forecast = termForecastRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("Term forecast not found"));
        forecast.setDeletedAt(Instant.now());
        termForecastRepository.save(forecast);
    }

    private double normalizeCoverage(Double value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    private JsonNode toJson(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.valueToTree(value);
    }

    private ClassSubject resolveClassSubject(TermForecastRequest request) {
        if (request.getClassSubjectId() != null) {
            return classSubjectRepository.findById(request.getClassSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Class subject not found"));
        }
        if (request.getSubjectId() == null) {
            throw new IllegalArgumentException("Subject is required");
        }

        if (request.getTeacherId() != null) {
            List<ClassSubject> links = classSubjectRepository
                .findBySubject_IdAndTeacher_IdAndDeletedAtIsNull(request.getSubjectId(), request.getTeacherId());
            if (!links.isEmpty()) {
                return links.get(0);
            }
        }

        List<ClassSubject> links = classSubjectRepository.findBySubject_IdAndDeletedAtIsNull(request.getSubjectId());
        if (links.isEmpty()) {
            throw new IllegalArgumentException("Class subject not found for subject");
        }
        return links.get(0);
    }
}

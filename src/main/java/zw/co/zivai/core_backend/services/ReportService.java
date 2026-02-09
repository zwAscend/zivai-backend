package zw.co.zivai.core_backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CurriculumForecastDto;
import zw.co.zivai.core_backend.dtos.CurriculumTopicForecastDto;
import zw.co.zivai.core_backend.dtos.TermForecastDto;
import zw.co.zivai.core_backend.dtos.TopicAnswerStat;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.TermForecast;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.repositories.AttemptAnswerRepository;
import zw.co.zivai.core_backend.repositories.QuestionRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.TermForecastRepository;
import zw.co.zivai.core_backend.repositories.TopicRepository;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TermForecastRepository termForecastRepository;
    private final ObjectMapper objectMapper;

    public CurriculumForecastDto getCurriculumForecast(UUID subjectId) {
        Subject subject = resolveSubject(subjectId);
        if (subject == null) {
            return CurriculumForecastDto.builder()
                .subjectId(null)
                .subjectName(null)
                .topics(List.of())
                .build();
        }

        List<Topic> topics = topicRepository.findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(subject.getId());
        Map<UUID, Long> questionCounts = questionRepository.findBySubject_IdAndDeletedAtIsNull(subject.getId()).stream()
            .filter(question -> question.getTopic() != null && question.getTopic().getId() != null)
            .collect(Collectors.groupingBy(q -> q.getTopic().getId(), Collectors.counting()));

        List<TopicAnswerStat> answerStats = attemptAnswerRepository.findTopicStatsBySubject(subject.getId()).stream()
            .filter(stat -> stat.getTopicId() != null)
            .collect(Collectors.toList());

        Map<UUID, Set<UUID>> attemptedQuestionsByTopic = new HashMap<>();
        Map<UUID, List<TopicAnswerStat>> statsByTopic = new HashMap<>();

        for (TopicAnswerStat stat : answerStats) {
            statsByTopic.computeIfAbsent(stat.getTopicId(), key -> new ArrayList<>()).add(stat);
            attemptedQuestionsByTopic
                .computeIfAbsent(stat.getTopicId(), key -> new HashSet<>())
                .add(stat.getAssessmentQuestionId());
        }

        List<CurriculumTopicForecastDto> topicDtos = topics.stream().map(topic -> {
            UUID topicId = topic.getId();
            long totalQuestions = questionCounts.getOrDefault(topicId, 0L);
            long attemptedQuestions = attemptedQuestionsByTopic.getOrDefault(topicId, Set.of()).size();
            double coveragePercent = totalQuestions == 0
                ? 0
                : (attemptedQuestions * 100.0) / totalQuestions;

            List<TopicAnswerStat> stats = statsByTopic.getOrDefault(topicId, List.of());
            double masteryPercent = calculateMastery(stats);
            long laggingStudents = calculateLaggingStudents(stats);

            String status;
            if (masteryPercent < 50 || coveragePercent < 40) {
                status = "critical";
            } else if (masteryPercent < 70 || coveragePercent < 70) {
                status = "risk";
            } else {
                status = "on_track";
            }

            String priority = switch (status) {
                case "critical" -> "high";
                case "risk" -> "medium";
                default -> "low";
            };

            return CurriculumTopicForecastDto.builder()
                .id(topicId.toString())
                .topic(topic.getName())
                .coveragePercent(roundOneDecimal(coveragePercent))
                .masteryPercent(roundOneDecimal(masteryPercent))
                .laggingStudents(laggingStudents)
                .status(status)
                .priority(priority)
                .build();
        }).sorted(Comparator.comparing(CurriculumTopicForecastDto::getCoveragePercent).reversed())
        .collect(Collectors.toList());

        return CurriculumForecastDto.builder()
            .subjectId(subject.getId().toString())
            .subjectName(subject.getName())
            .topics(topicDtos)
            .build();
    }

    public TermForecastDto getTermForecast(UUID subjectId, String term, String academicYear, UUID forecastId) {
        Subject subject = resolveSubject(subjectId);
        TermForecast forecast = resolveForecast(subject != null ? subject.getId() : null, term, academicYear, forecastId);
        if (subject == null && forecast != null && forecast.getClassSubject() != null) {
            subject = forecast.getClassSubject().getSubject();
        }
        if (subject == null) {
            return TermForecastDto.builder()
                .subjectId(null)
                .subjectName(null)
                .term(term)
                .expectedCoveragePercent(0)
                .topics(List.of())
                .build();
        }

        CurriculumForecastDto curriculum = getCurriculumForecast(subject.getId());
        List<String> expectedTopicIds = extractExpectedTopicIds(forecast != null ? forecast.getExpectedTopicIds() : null);
        if (forecast != null && expectedTopicIds.isEmpty()) {
            String rawExpected = termForecastRepository.findExpectedTopicIdsTextById(forecast.getId());
            expectedTopicIds = extractExpectedTopicIdsFromText(rawExpected);
        }
        List<CurriculumTopicForecastDto> filteredTopics = curriculum.getTopics();
        if (!expectedTopicIds.isEmpty()) {
            Set<String> expectedSet = new HashSet<>(expectedTopicIds);
            filteredTopics = curriculum.getTopics().stream()
                .filter(topic -> expectedSet.contains(topic.getId()))
                .collect(Collectors.toList());
        }

        return TermForecastDto.builder()
            .subjectId(subject.getId().toString())
            .subjectName(subject.getName())
            .term(forecast != null ? forecast.getTerm() : term)
            .expectedCoveragePercent(forecast != null && forecast.getExpectedCoveragePct() != null ? forecast.getExpectedCoveragePct() : 0)
            .expectedTopicIds(expectedTopicIds)
            .topics(filteredTopics)
            .build();
    }

    private TermForecast resolveForecast(UUID subjectId, String term, String academicYear, UUID forecastId) {
        if (forecastId != null) {
            return termForecastRepository.findByIdAndDeletedAtIsNull(forecastId).orElse(null);
        }
        if (subjectId == null || term == null) {
            return null;
        }
        if (academicYear != null && !academicYear.isBlank()) {
            return termForecastRepository
                .findLatestBySubjectTermAndYear(subjectId, term, academicYear)
                .stream()
                .findFirst()
                .orElse(null);
        }
        return termForecastRepository.findLatestBySubjectAndTerm(subjectId, term).stream()
            .findFirst()
            .orElse(null);
    }

    private Subject resolveSubject(UUID subjectId) {
        if (subjectId != null) {
            return subjectRepository.findByIdAndDeletedAtIsNull(subjectId).orElse(null);
        }
        return subjectRepository.findAllByDeletedAtIsNull().stream().findFirst().orElse(null);
    }

    private double calculateMastery(List<TopicAnswerStat> stats) {
        if (stats.isEmpty()) {
            return 0;
        }
        double total = 0;
        int count = 0;
        for (TopicAnswerStat stat : stats) {
            if (stat.getMaxScore() == null || stat.getMaxScore() == 0) {
                continue;
            }
            double score = stat.getScore() != null ? stat.getScore() : 0;
            total += (score / stat.getMaxScore()) * 100.0;
            count += 1;
        }
        return count == 0 ? 0 : total / count;
    }

    private long calculateLaggingStudents(List<TopicAnswerStat> stats) {
        if (stats.isEmpty()) {
            return 0;
        }
        Map<UUID, double[]> studentTotals = new HashMap<>();
        for (TopicAnswerStat stat : stats) {
            if (stat.getStudentId() == null || stat.getMaxScore() == null || stat.getMaxScore() == 0) {
                continue;
            }
            double score = stat.getScore() != null ? stat.getScore() : 0;
            studentTotals.computeIfAbsent(stat.getStudentId(), key -> new double[] {0, 0});
            double[] totals = studentTotals.get(stat.getStudentId());
            totals[0] += score;
            totals[1] += stat.getMaxScore();
        }
        return studentTotals.values().stream()
            .filter(totals -> totals[1] > 0 && (totals[0] / totals[1]) * 100.0 < 50)
            .count();
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
        return parseExpectedTopicText(node.toString());
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

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

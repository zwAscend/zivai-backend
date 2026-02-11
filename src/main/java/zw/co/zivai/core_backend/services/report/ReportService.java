package zw.co.zivai.core_backend.services.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.reports.ClassReportDto;
import zw.co.zivai.core_backend.dtos.reports.CurriculumForecastDto;
import zw.co.zivai.core_backend.dtos.reports.CurriculumTopicForecastDto;
import zw.co.zivai.core_backend.dtos.reports.StudentReportAssessmentDto;
import zw.co.zivai.core_backend.dtos.reports.StudentReportDto;
import zw.co.zivai.core_backend.dtos.reports.StudentTopicMasteryDto;
import zw.co.zivai.core_backend.dtos.termforecast.TermForecastDto;
import zw.co.zivai.core_backend.dtos.assessments.TopicAnswerStat;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.TermForecast;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.assessments.AttemptAnswerRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassRepository;
import zw.co.zivai.core_backend.repositories.assessments.QuestionRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.termforecast.TermForecastRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TermForecastRepository termForecastRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
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
        Map<UUID, Long> questionCounts = questionRepository.countQuestionsByTopic(subject.getId()).stream()
            .filter(row -> row != null && row.length >= 2 && row[0] != null)
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> row[1] instanceof Number number ? number.longValue() : 0L,
                (left, right) -> left
            ));

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

    public ClassReportDto getClassReport(UUID subjectId, UUID classId) {
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findSubmittedForReport(subjectId, classId);

        Map<UUID, double[]> studentTotals = new HashMap<>();
        for (AssessmentAttempt attempt : attempts) {
            Double percent = percentFromAttempt(attempt);
            if (percent == null) {
                continue;
            }
            UUID studentId = attempt.getAssessmentEnrollment().getStudent().getId();
            double[] totals = studentTotals.computeIfAbsent(studentId, key -> new double[] {0, 0});
            totals[0] += percent;
            totals[1] += 1;
        }

        List<Double> studentAverages = studentTotals.values().stream()
            .filter(totals -> totals[1] > 0)
            .map(totals -> totals[0] / totals[1])
            .toList();

        double classAverage = studentAverages.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        Map<String, Long> gradeDistribution = new LinkedHashMap<>();
        gradeDistribution.put("A", 0L);
        gradeDistribution.put("B", 0L);
        gradeDistribution.put("C", 0L);
        gradeDistribution.put("D", 0L);
        gradeDistribution.put("E", 0L);
        gradeDistribution.put("U", 0L);

        for (double avg : studentAverages) {
            String grade = gradeFromPercent(avg);
            gradeDistribution.compute(grade, (key, value) -> value == null ? 1L : value + 1);
        }

        Subject subject = resolveSubject(subjectId);
        if (subject == null) {
            subject = attempts.stream()
                .map(attempt -> attempt.getAssessmentEnrollment().getAssessmentAssignment().getAssessment().getSubject())
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
        }

        List<CurriculumTopicForecastDto> masteryGaps = List.of();
        if (subject != null) {
            CurriculumForecastDto forecast = getCurriculumForecast(subject.getId());
            masteryGaps = forecast.getTopics().stream()
                .sorted(Comparator.comparing(CurriculumTopicForecastDto::getMasteryPercent))
                .limit(5)
                .toList();
        }

        ClassEntity classEntity = classId != null
            ? classRepository.findByIdAndDeletedAtIsNull(classId).orElse(null)
            : null;

        return ClassReportDto.builder()
            .subjectId(subject != null ? subject.getId().toString() : null)
            .subjectName(subject != null ? subject.getName() : null)
            .classId(classEntity != null ? classEntity.getId().toString() : null)
            .className(classEntity != null ? classEntity.getName() : null)
            .classAveragePercent(roundOneDecimal(classAverage))
            .predictedGrade(gradeFromPercent(classAverage))
            .studentCount(studentAverages.size())
            .assessmentCount(attempts.size())
            .gradeDistribution(gradeDistribution)
            .masteryGaps(masteryGaps)
            .build();
    }

    public StudentReportDto getStudentReport(UUID studentId, UUID subjectId) {
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId).orElse(null);
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findSubmittedByStudentForReport(studentId, subjectId);

        List<StudentReportAssessmentDto> assessmentSummaries = attempts.stream()
            .map(attempt -> {
                AssessmentAssignment assignment = attempt.getAssessmentEnrollment().getAssessmentAssignment();
                Assessment assessment = assignment.getAssessment();
                Double percent = percentFromAttempt(attempt);
                Double score = attempt.getFinalScore() != null ? attempt.getFinalScore() : attempt.getTotalScore();
                Double maxScore = resolveMaxScore(attempt);
                return StudentReportAssessmentDto.builder()
                    .assessmentId(assessment != null ? assessment.getId().toString() : null)
                    .assessmentName(assessment != null ? assessment.getName() : null)
                    .assessmentType(assessment != null ? assessment.getAssessmentType() : null)
                    .score(score)
                    .maxScore(maxScore)
                    .percent(percent)
                    .submittedAt(attempt.getSubmittedAt())
                    .build();
            })
            .toList();

        double averagePercent = assessmentSummaries.stream()
            .map(StudentReportAssessmentDto::getPercent)
            .filter(value -> value != null)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        UUID resolvedSubjectId = subjectId;
        if (resolvedSubjectId == null) {
            resolvedSubjectId = attempts.stream()
                .map(attempt -> attempt.getAssessmentEnrollment().getAssessmentAssignment().getAssessment().getSubject())
                .filter(value -> value != null)
                .map(Subject::getId)
                .findFirst()
                .orElse(null);
        }

        Subject subject = resolveSubject(resolvedSubjectId);

        List<StudentTopicMasteryDto> masteryGaps = List.of();
        if (resolvedSubjectId != null) {
            List<TopicAnswerStat> stats =
                attemptAnswerRepository.findTopicStatsBySubjectAndStudent(resolvedSubjectId, studentId);
            Map<UUID, double[]> totalsByTopic = new HashMap<>();
            for (TopicAnswerStat stat : stats) {
                if (stat.getTopicId() == null || stat.getMaxScore() == null || stat.getMaxScore() == 0) {
                    continue;
                }
                double score = stat.getScore() != null ? stat.getScore() : 0;
                double[] totals = totalsByTopic.computeIfAbsent(stat.getTopicId(), key -> new double[] {0, 0});
                totals[0] += score;
                totals[1] += stat.getMaxScore();
            }
            Map<UUID, Topic> topicMap = topicRepository
                .findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(resolvedSubjectId).stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));

            masteryGaps = totalsByTopic.entrySet().stream()
                .map(entry -> {
                    UUID topicId = entry.getKey();
                    double[] totals = entry.getValue();
                    double masteryPercent = totals[1] == 0 ? 0 : (totals[0] / totals[1]) * 100.0;
                    String status;
                    if (masteryPercent < 50) {
                        status = "critical";
                    } else if (masteryPercent < 70) {
                        status = "risk";
                    } else {
                        status = "on_track";
                    }
                    String priority = switch (status) {
                        case "critical" -> "high";
                        case "risk" -> "medium";
                        default -> "low";
                    };
                    Topic topic = topicMap.get(topicId);
                    return StudentTopicMasteryDto.builder()
                        .topicId(topicId.toString())
                        .topicName(topic != null ? topic.getName() : null)
                        .masteryPercent(roundOneDecimal(masteryPercent))
                        .status(status)
                        .priority(priority)
                        .build();
                })
                .sorted(Comparator.comparing(StudentTopicMasteryDto::getMasteryPercent))
                .limit(5)
                .toList();
        }

        return StudentReportDto.builder()
            .studentId(student != null ? student.getId().toString() : studentId.toString())
            .studentName(student != null ? student.getFirstName() + " " + student.getLastName() : null)
            .subjectId(subject != null ? subject.getId().toString() : null)
            .subjectName(subject != null ? subject.getName() : null)
            .averagePercent(roundOneDecimal(averagePercent))
            .predictedGrade(gradeFromPercent(averagePercent))
            .assessmentCount(assessmentSummaries.size())
            .assessments(assessmentSummaries)
            .masteryGaps(masteryGaps)
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

    private String gradeFromPercent(double percent) {
        if (percent >= 80) return "A";
        if (percent >= 70) return "B";
        if (percent >= 60) return "C";
        if (percent >= 50) return "D";
        if (percent >= 40) return "E";
        return "U";
    }

    private Double resolveMaxScore(AssessmentAttempt attempt) {
        if (attempt.getMaxScore() != null) {
            return attempt.getMaxScore();
        }
        AssessmentAssignment assignment = attempt.getAssessmentEnrollment().getAssessmentAssignment();
        Assessment assessment = assignment != null ? assignment.getAssessment() : null;
        return assessment != null ? assessment.getMaxScore() : null;
    }

    private Double percentFromAttempt(AssessmentAttempt attempt) {
        Double maxScore = resolveMaxScore(attempt);
        if (maxScore == null || maxScore == 0) {
            return null;
        }
        Double score = attempt.getFinalScore() != null ? attempt.getFinalScore() : attempt.getTotalScore();
        if (score == null) {
            return null;
        }
        return (score / maxScore) * 100.0;
    }
}

package zw.co.zivai.core_backend.dtos.termforecast;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TermForecastResponse {
    private UUID id;
    private String term;
    private String academicYear;
    private Double expectedCoveragePct;
    private List<String> expectedTopicIds;
    private String notes;
    private ClassSubjectSnapshot classSubject;

    @Data
    @Builder
    public static class ClassSubjectSnapshot {
        private UUID id;
        private SubjectSnapshot subject;
        private ClassSnapshot classEntity;
    }

    @Data
    @Builder
    public static class SubjectSnapshot {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    public static class ClassSnapshot {
        private UUID id;
        private String name;
    }
}

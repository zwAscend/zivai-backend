package zw.co.zivai.core_backend.dtos.termforecast;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class TermForecastRequest {
    private UUID classSubjectId;
    private UUID subjectId;
    private UUID teacherId;
    private String term;
    private String academicYear;
    private Double expectedCoveragePercent;
    private List<UUID> expectedTopicIds;
    private String notes;
}

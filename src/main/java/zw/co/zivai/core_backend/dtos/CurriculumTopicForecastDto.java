package zw.co.zivai.core_backend.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurriculumTopicForecastDto {
    private String id;
    private String topic;
    private double coveragePercent;
    private double masteryPercent;
    private long laggingStudents;
    private String status;
    private String priority;
}

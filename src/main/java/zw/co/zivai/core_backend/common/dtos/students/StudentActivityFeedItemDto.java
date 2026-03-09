package zw.co.zivai.core_backend.common.dtos.students;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentActivityFeedItemDto {
    String id;
    String activityType;
    String sourceId;
    String title;
    String subjectId;
    String subjectName;
    Instant occurredAt;
    String level;
    Double progressPercent;
    Integer correctCount;
    Integer totalCount;
    Double score;
    Double maxScore;
    Integer timeMinutes;
}

package zw.co.zivai.core_backend.dtos.students;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentSubjectOverviewTopicDto {
    String topicId;
    String code;
    String name;
    Integer sequenceIndex;
    double masteryPercent;
    long questionCount;
}

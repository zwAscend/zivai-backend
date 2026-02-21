package zw.co.zivai.core_backend.dtos.students;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentSubjectOverviewUnitDto {
    String unitId;
    int unitNumber;
    String code;
    String title;
    long topicCount;
    long questionCount;
    double masteryPercent;
    List<StudentSubjectOverviewTopicDto> topics;
}

package zw.co.zivai.core_backend.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentTopicMasteryDto {
    String topicId;
    String topicName;
    double masteryPercent;
    String status;
    String priority;
}

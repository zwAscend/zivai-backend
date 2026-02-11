package zw.co.zivai.core_backend.dtos.development;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReteachCardDto {
    String id;
    String subjectId;
    String subjectName;
    String topicId;
    String topicName;
    String title;
    String issueSummary;
    String recommendedActions;
    String priority;
    String status;
    List<String> affectedStudentIds;
    int affectedStudents;
    Instant createdAt;
    Instant updatedAt;
}

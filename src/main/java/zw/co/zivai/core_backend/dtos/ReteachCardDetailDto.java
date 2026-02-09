package zw.co.zivai.core_backend.dtos;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReteachCardDetailDto {
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
    List<StudentRefDto> affectedStudents;
    int affectedStudentsCount;
    Instant createdAt;
    Instant updatedAt;
}

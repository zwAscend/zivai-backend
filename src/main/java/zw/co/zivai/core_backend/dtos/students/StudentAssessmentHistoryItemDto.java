package zw.co.zivai.core_backend.dtos.students;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentAssessmentHistoryItemDto {
    String enrollmentId;
    String assignmentId;
    String assessmentId;
    String assessmentName;
    String assessmentType;
    String subjectId;
    String subjectName;
    Instant startTime;
    Instant dueTime;
    boolean published;
    String status;
    String submissionId;
    Integer attemptNumber;
    Instant submittedAt;
    Instant gradedAt;
    Double score;
    Double maxScore;
    Double expectedMark;
    Double actualMark;
    String grade;
    String feedback;
}

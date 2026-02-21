package zw.co.zivai.core_backend.dtos.assessments;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubjectTopicAnswerStat {
    private UUID subjectId;
    private UUID topicId;
    private Double score;
    private Double maxScore;
}

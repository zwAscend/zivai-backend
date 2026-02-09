package zw.co.zivai.core_backend.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopicAnswerStat {
    private UUID topicId;
    private UUID studentId;
    private UUID assessmentQuestionId;
    private Double score;
    private Double maxScore;
}

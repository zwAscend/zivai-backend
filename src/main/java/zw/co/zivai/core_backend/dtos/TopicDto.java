package zw.co.zivai.core_backend.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicDto {
    private String id;
    private String subjectId;
    private String code;
    private String name;
    private String description;
    private String objectives;
    private Integer sequenceIndex;
}

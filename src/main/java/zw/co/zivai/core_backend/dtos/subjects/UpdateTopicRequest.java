package zw.co.zivai.core_backend.dtos.subjects;

import lombok.Data;

@Data
public class UpdateTopicRequest {
    private String code;
    private String name;
    private String description;
    private String objectives;
    private Integer sequenceIndex;
}

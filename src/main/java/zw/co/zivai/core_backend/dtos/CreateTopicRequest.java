package zw.co.zivai.core_backend.dtos;

import lombok.Data;

@Data
public class CreateTopicRequest {
    private String code;
    private String name;
    private String description;
    private String objectives;
    private Integer sequenceIndex;
}

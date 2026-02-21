package zw.co.zivai.core_backend.dtos.subjects;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TopicWithResourcesDto {
    String id;
    String subjectId;
    String code;
    String name;
    String description;
    String objectives;
    Integer sequenceIndex;
    List<TopicResourceDto> resources;
}

package zw.co.zivai.core_backend.common.dtos.resources;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ResourceTopicIdsRequest {
    private List<UUID> topicIds;
}

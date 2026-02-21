package zw.co.zivai.core_backend.dtos.subjects;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TopicResourceDto {
    String id;
    String name;
    String originalName;
    String mimeType;
    String type;
    String url;
    String contentType;
    String status;
    Instant publishAt;
    List<String> topicIds;
}

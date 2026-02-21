package zw.co.zivai.core_backend.dtos.peerstudy;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePeerStudyRequest {
    private String subjectId;
    private String topicId;
    private String topic;
    private String type;
    private String note;
    private Instant preferredTime;
    private Integer maxParticipants;
    private String createdBy;
}

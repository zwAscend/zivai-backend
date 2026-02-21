package zw.co.zivai.core_backend.dtos.peerstudy;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PeerStudyRequestDetailDto {
    String id;
    String subjectId;
    String subjectName;
    String topicId;
    String topic;
    String type;
    String note;
    Instant preferredTime;
    String status;
    Integer maxParticipants;
    int participants;
    String createdById;
    String createdByName;
    boolean joined;
    List<PeerStudyMemberDto> members;
    Instant createdAt;
    Instant updatedAt;
}

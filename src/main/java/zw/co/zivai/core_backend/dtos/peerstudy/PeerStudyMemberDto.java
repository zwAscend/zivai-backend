package zw.co.zivai.core_backend.dtos.peerstudy;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PeerStudyMemberDto {
    String userId;
    String firstName;
    String lastName;
    String role;
    Instant joinedAt;
}

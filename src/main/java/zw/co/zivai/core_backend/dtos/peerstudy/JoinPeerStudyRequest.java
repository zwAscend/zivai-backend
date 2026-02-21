package zw.co.zivai.core_backend.dtos.peerstudy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JoinPeerStudyRequest {
    private String studentId;
}

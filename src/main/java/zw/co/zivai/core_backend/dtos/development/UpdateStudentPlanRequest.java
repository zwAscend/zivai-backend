package zw.co.zivai.core_backend.dtos.development;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStudentPlanRequest {
    private String planId;
    private String subjectId;
    private Double currentProgress;
    private String status;
    private Boolean current;
    private Instant startDate;
    private Instant completionDate;
}

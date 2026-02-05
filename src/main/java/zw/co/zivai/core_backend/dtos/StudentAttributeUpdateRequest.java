package zw.co.zivai.core_backend.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentAttributeUpdateRequest {
    private String attributeId;
    private Double current;
    private Double potential;
}

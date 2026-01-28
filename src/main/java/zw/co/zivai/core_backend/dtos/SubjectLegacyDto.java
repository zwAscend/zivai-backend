package zw.co.zivai.core_backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubjectLegacyDto {
    @JsonProperty("_id")
    String _id;
    String code;
    String name;
    String description;
    String teacher;
}

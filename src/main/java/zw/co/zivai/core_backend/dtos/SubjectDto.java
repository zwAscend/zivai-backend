package zw.co.zivai.core_backend.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubjectDto {
    String id;
    String code;
    String name;
    String description;
    String teacher;
}

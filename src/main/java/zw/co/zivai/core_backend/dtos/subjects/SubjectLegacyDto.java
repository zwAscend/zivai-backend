package zw.co.zivai.core_backend.dtos.subjects;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubjectLegacyDto {
    String id;
    String code;
    String name;
    String description;
    String teacher;
}

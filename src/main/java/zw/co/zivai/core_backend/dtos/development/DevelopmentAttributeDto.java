package zw.co.zivai.core_backend.dtos.development;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DevelopmentAttributeDto {
    String id;
    String name;
    String description;
    String category;
    String subjectId;
    String attributeId;
}

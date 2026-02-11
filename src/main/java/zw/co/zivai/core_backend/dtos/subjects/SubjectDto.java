package zw.co.zivai.core_backend.dtos.subjects;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubjectDto {
    String id;
    String code;
    String name;
    String examBoardCode;
    String description;
    boolean active;
    List<String> grades;
    List<String> teachers;
}

package zw.co.zivai.core_backend.dtos.subjects;

import java.util.List;

import lombok.Data;

@Data
public class CreateSubjectRequest {
    private String code;
    private String name;
    private String examBoardCode;
    private String description;
    private List<String> grades;
    private Object subjectAttributes;
    private boolean active = true;
}

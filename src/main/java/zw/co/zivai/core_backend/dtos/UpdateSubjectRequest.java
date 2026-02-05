package zw.co.zivai.core_backend.dtos;

import java.util.List;

import lombok.Data;

@Data
public class UpdateSubjectRequest {
    private String code;
    private String name;
    private String examBoardCode;
    private String description;
    private List<String> grades;
    private Object subjectAttributes;
    private Boolean active;
}

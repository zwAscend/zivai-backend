package zw.co.zivai.core_backend.dtos;

import lombok.Data;

@Data
public class CreateSubjectRequest {
    private String code;
    private String name;
    private String examBoardCode;
    private String description;
    private boolean active = true;
}

package zw.co.zivai.core_backend.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateClassRequest {
    private UUID schoolId;
    private String code;
    private String name;
    private String gradeLevel;
    private String academicYear;
    private UUID homeroomTeacherId;
}

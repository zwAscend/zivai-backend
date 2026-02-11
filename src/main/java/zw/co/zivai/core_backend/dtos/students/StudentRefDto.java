package zw.co.zivai.core_backend.dtos.students;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentRefDto {
    String id;
    String firstName;
    String lastName;
}

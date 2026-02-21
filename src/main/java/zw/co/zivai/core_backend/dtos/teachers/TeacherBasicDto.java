package zw.co.zivai.core_backend.dtos.teachers;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherBasicDto {
    String id;
    String firstName;
    String lastName;
    String email;
    String phoneNumber;
    List<String> roles;
}

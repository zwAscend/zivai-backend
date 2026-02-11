package zw.co.zivai.core_backend.dtos.students;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentDto {
    String id;
    String firstName;
    String lastName;
    String email;
    double overall;
    String strength;
    String performance;
    String engagement;
    List<String> subjects;
}

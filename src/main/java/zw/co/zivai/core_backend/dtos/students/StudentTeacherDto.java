package zw.co.zivai.core_backend.dtos.students;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentTeacherDto {
    String id;
    String firstName;
    String lastName;
    String email;
    List<String> subjectNames;
    List<String> classNames;
    List<String> homeroomClassNames;
}

package zw.co.zivai.core_backend.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentDto {
    @JsonProperty("_id")
    String _id;
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

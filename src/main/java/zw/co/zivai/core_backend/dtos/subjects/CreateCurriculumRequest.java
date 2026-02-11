package zw.co.zivai.core_backend.dtos.subjects;

import java.util.List;

import lombok.Data;

@Data
public class CreateCurriculumRequest {
    private List<CreateTopicRequest> topics;
    private boolean replaceExisting = false;
}

package zw.co.zivai.core_backend.common.dtos.subjects;

import java.util.List;

import lombok.Data;

@Data
public class CreateCurriculumRequest {
    private List<CreateTopicRequest> topics;
    private boolean replaceExisting = false;
}

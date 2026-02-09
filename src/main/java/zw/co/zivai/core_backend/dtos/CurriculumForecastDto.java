package zw.co.zivai.core_backend.dtos;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurriculumForecastDto {
    private String subjectId;
    private String subjectName;
    private List<CurriculumTopicForecastDto> topics;
}

package zw.co.zivai.core_backend.common.dtos.termforecast;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import zw.co.zivai.core_backend.common.dtos.reports.CurriculumTopicForecastDto;

@Data
@Builder
public class TermForecastDto {
    private String subjectId;
    private String subjectName;
    private String term;
    private double expectedCoveragePercent;
    private List<String> expectedTopicIds;
    private List<CurriculumTopicForecastDto> topics;
}

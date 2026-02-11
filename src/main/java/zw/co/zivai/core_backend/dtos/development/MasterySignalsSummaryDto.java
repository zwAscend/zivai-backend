package zw.co.zivai.core_backend.dtos.development;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MasterySignalsSummaryDto {
    int totalStudents;
    int excellent;
    int good;
    int average;
    int needsImprovement;
    double averageOverall;
}

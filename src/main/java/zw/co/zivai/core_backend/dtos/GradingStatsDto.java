package zw.co.zivai.core_backend.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradingStatsDto {
    private long totalSubmissions;
    private long autoGradedCount;
    private long teacherReviewedCount;
    private double averageScore;
    private double averageConfidence;
}

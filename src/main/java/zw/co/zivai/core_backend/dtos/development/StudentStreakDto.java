package zw.co.zivai.core_backend.dtos.development;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentStreakDto {
    String studentId;
    int streakDays;
    int streakWeeks;
    int level;
    int progressToNextWeek;
    boolean activeToday;
    LocalDate lastActiveDate;
}


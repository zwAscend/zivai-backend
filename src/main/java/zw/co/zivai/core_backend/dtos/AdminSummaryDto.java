package zw.co.zivai.core_backend.dtos;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSummaryDto {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalAdmins;
    private long totalSubjects;
    private long activeSubjects;
    private long totalClasses;
    private long totalSchools;
    private long totalClassSubjectLinks;
    private List<AdminSummaryRecentUserDto> recentUsers;
}

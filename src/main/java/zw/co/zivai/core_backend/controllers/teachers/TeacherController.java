package zw.co.zivai.core_backend.controllers.teachers;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.common.PageResponse;
import zw.co.zivai.core_backend.dtos.teachers.TeacherAssessmentOverviewDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherBasicDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherClassDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherDashboardDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherStudentProfileSummaryDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherStudentSummaryDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherSubjectDto;
import zw.co.zivai.core_backend.services.teachers.TeacherService;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    @GetMapping("/{teacherId}")
    public TeacherBasicDto getTeacher(@PathVariable UUID teacherId) {
        return teacherService.getTeacher(teacherId);
    }

    @GetMapping("/{teacherId}/assessments/overview")
    public List<TeacherAssessmentOverviewDto> getAssessmentsOverview(@PathVariable UUID teacherId,
                                                                     @RequestParam(required = false) UUID subjectId,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) UUID studentId,
                                                                     @RequestParam(required = false) String search,
                                                                     @RequestParam(required = false) String from,
                                                                     @RequestParam(required = false) String to) {
        return teacherService.getAssessmentOverview(teacherId, subjectId, status, studentId, search, from, to);
    }

    @GetMapping("/{teacherId}/students/summary")
    public PageResponse<TeacherStudentSummaryDto> getStudentsSummary(@PathVariable UUID teacherId,
                                                                     @RequestParam(required = false) UUID subjectId,
                                                                     @RequestParam(required = false) UUID classId,
                                                                     @RequestParam(required = false) String performance,
                                                                     @RequestParam(required = false) String planStatus,
                                                                     @RequestParam(required = false, name = "q") String query,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return teacherService.getStudentsSummary(teacherId, subjectId, classId, performance, planStatus, query, page, size);
    }

    @GetMapping("/{teacherId}/students/{studentId}/profile-summary")
    public TeacherStudentProfileSummaryDto getStudentProfileSummary(@PathVariable UUID teacherId,
                                                                    @PathVariable UUID studentId,
                                                                    @RequestParam(required = false) UUID subjectId) {
        return teacherService.getStudentProfileSummary(teacherId, studentId, subjectId);
    }

    @GetMapping("/{teacherId}/dashboard")
    public TeacherDashboardDto getDashboard(@PathVariable UUID teacherId,
                                            @RequestParam(required = false) UUID subjectId) {
        return teacherService.getDashboard(teacherId, subjectId);
    }

    @GetMapping("/me")
    public TeacherBasicDto getMe(@RequestParam UUID teacherId) {
        return teacherService.getTeacher(teacherId);
    }

    @GetMapping("/me/subjects")
    public List<TeacherSubjectDto> getMySubjects(@RequestParam UUID teacherId) {
        return teacherService.getTeacherSubjects(teacherId);
    }

    @GetMapping("/me/classes")
    public List<TeacherClassDto> getMyClasses(@RequestParam UUID teacherId,
                                              @RequestParam(required = false) UUID subjectId) {
        return teacherService.getTeacherClasses(teacherId, subjectId);
    }
}

package zw.co.zivai.core_backend.common.controllers.students;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.development.DevelopmentPlanDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentAssessmentDetailDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentAssessmentHistoryItemDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentActivityFeedItemDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPlanRuntimeProgressDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPlanRuntimeProgressRequest;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeAnswerRequest;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeAnswerResultDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeSessionDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentSubjectOverviewDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentTeacherDto;
import zw.co.zivai.core_backend.common.dtos.students.StartStudentPracticeSessionRequest;
import zw.co.zivai.core_backend.common.services.development.DevelopmentService;
import zw.co.zivai.core_backend.common.services.students.StudentService;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;
    private final DevelopmentService developmentService;

    @GetMapping
    public List<StudentDto> list(@RequestParam(required = false) UUID subjectId,
                                 @RequestParam(required = false) UUID classId,
                                 @RequestParam(required = false) UUID classSubjectId) {
        return studentService.list(subjectId, classId, classSubjectId);
    }

    @GetMapping("/{id}")
    public StudentDto get(@PathVariable UUID id) {
        return studentService.get(id);
    }

    @GetMapping("/{id}/teachers")
    public List<StudentTeacherDto> getTeachers(@PathVariable UUID id) {
        return studentService.getTeachers(id);
    }

    @GetMapping("/{studentId}/assessments")
    public List<StudentAssessmentHistoryItemDto> getStudentAssessmentHistory(@PathVariable UUID studentId,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(required = false) UUID subjectId,
                                                                             @RequestParam(required = false) String from,
                                                                             @RequestParam(required = false) String to) {
        return studentService.getStudentAssessments(studentId, status, subjectId, from, to);
    }

    @GetMapping("/{studentId}/assessments/{assessmentId}")
    public StudentAssessmentDetailDto getStudentAssessment(@PathVariable UUID studentId,
                                                           @PathVariable UUID assessmentId) {
        return studentService.getStudentAssessment(studentId, assessmentId);
    }

    @GetMapping("/{studentId}/activity-feed")
    public List<StudentActivityFeedItemDto> getStudentActivityFeed(@PathVariable UUID studentId,
                                                                   @RequestParam(required = false) UUID subjectId,
                                                                   @RequestParam(required = false) String from,
                                                                   @RequestParam(required = false) String to,
                                                                   @RequestParam(required = false) Integer limit) {
        return studentService.getStudentActivityFeed(studentId, subjectId, from, to, limit);
    }

    @GetMapping("/{studentId}/subjects/{subjectId}/overview")
    public StudentSubjectOverviewDto getSubjectOverview(@PathVariable UUID studentId,
                                                        @PathVariable UUID subjectId) {
        return studentService.getSubjectOverview(studentId, subjectId);
    }

    @PostMapping("/{studentId}/subjects/{subjectId}/practice-sessions")
    public StudentPracticeSessionDto startPracticeSession(@PathVariable UUID studentId,
                                                          @PathVariable UUID subjectId,
                                                          @RequestBody(required = false) StartStudentPracticeSessionRequest request) {
        return studentService.startPracticeSession(studentId, subjectId, request);
    }

    @PostMapping("/{studentId}/practice-sessions/{sessionId}/answers")
    public StudentPracticeAnswerResultDto submitPracticeAnswer(@PathVariable UUID studentId,
                                                               @PathVariable UUID sessionId,
                                                               @RequestBody StudentPracticeAnswerRequest request) {
        return studentService.submitPracticeAnswer(studentId, sessionId, request);
    }

    @PostMapping("/{studentId}/practice-sessions/{sessionId}/complete")
    public StudentPracticeSessionDto completePracticeSession(@PathVariable UUID studentId,
                                                             @PathVariable UUID sessionId) {
        return studentService.completePracticeSession(studentId, sessionId);
    }

    @GetMapping("/{studentId}/practice-sessions/history")
    public List<StudentPracticeSessionDto> getPracticeSessionHistory(@PathVariable UUID studentId,
                                                                     @RequestParam(required = false) UUID subjectId,
                                                                     @RequestParam(required = false) Integer limit) {
        return studentService.getPracticeSessionHistory(studentId, subjectId, limit);
    }

    @PatchMapping("/{studentId}/plans/{studentPlanId}/runtime")
    public StudentPlanRuntimeProgressDto updateStudentPlanRuntime(@PathVariable UUID studentId,
                                                                  @PathVariable UUID studentPlanId,
                                                                  @RequestBody StudentPlanRuntimeProgressRequest request) {
        return studentService.updateStudentPlanRuntime(studentId, studentPlanId, request);
    }

    @GetMapping("/{studentId}/development-plans")
    public List<DevelopmentPlanDto> getPublishedDevelopmentPlans(@PathVariable UUID studentId,
                                                                 @RequestParam(required = false) UUID subjectId) {
        return developmentService.getPublishedStudentPlans(studentId, subjectId);
    }
}

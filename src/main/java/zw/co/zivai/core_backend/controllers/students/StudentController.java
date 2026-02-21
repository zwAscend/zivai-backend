package zw.co.zivai.core_backend.controllers.students;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.students.StudentAssessmentDetailDto;
import zw.co.zivai.core_backend.dtos.students.StudentAssessmentHistoryItemDto;
import zw.co.zivai.core_backend.dtos.students.StudentDto;
import zw.co.zivai.core_backend.dtos.students.StudentSubjectOverviewDto;
import zw.co.zivai.core_backend.dtos.students.StudentTeacherDto;
import zw.co.zivai.core_backend.services.students.StudentService;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

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

    @GetMapping("/{studentId}/subjects/{subjectId}/overview")
    public StudentSubjectOverviewDto getSubjectOverview(@PathVariable UUID studentId,
                                                        @PathVariable UUID subjectId) {
        return studentService.getSubjectOverview(studentId, subjectId);
    }
}

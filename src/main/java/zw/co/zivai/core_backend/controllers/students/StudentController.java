package zw.co.zivai.core_backend.controllers.students;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.students.StudentDto;
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
}

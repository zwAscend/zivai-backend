package zw.co.zivai.core_backend.controllers.students;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.StudentDto;
import zw.co.zivai.core_backend.services.StudentService;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @GetMapping
    public List<StudentDto> list() {
        return studentService.list();
    }

    @GetMapping("/{id}")
    public StudentDto get(@PathVariable UUID id) {
        return studentService.get(id);
    }
}

package zw.co.zivai.core_backend.controllers.classroom;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.classroom.CreateClassRequest;
import zw.co.zivai.core_backend.dtos.classroom.UpdateClassRequest;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.services.classroom.ClassService;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {
    private final ClassService classService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassEntity create(@RequestBody CreateClassRequest request) {
        return classService.create(request);
    }

    @GetMapping
    public List<ClassEntity> list() {
        return classService.list();
    }

    @GetMapping("/{id}")
    public ClassEntity get(@PathVariable UUID id) {
        return classService.get(id);
    }

    @PutMapping("/{id}")
    public ClassEntity update(@PathVariable UUID id, @RequestBody UpdateClassRequest request) {
        return classService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        classService.delete(id);
    }
}

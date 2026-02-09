package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateReteachCardRequest;
import zw.co.zivai.core_backend.dtos.ReteachCardDetailDto;
import zw.co.zivai.core_backend.dtos.ReteachCardDto;
import zw.co.zivai.core_backend.dtos.UpdateReteachCardRequest;
import zw.co.zivai.core_backend.services.ReteachCardService;

@RestController
@RequestMapping("/api/reteach-cards")
@RequiredArgsConstructor
public class ReteachCardController {
    private final ReteachCardService reteachCardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReteachCardDto create(@RequestBody CreateReteachCardRequest request) {
        return reteachCardService.create(request);
    }

    @GetMapping
    public List<ReteachCardDto> list(@RequestParam(required = false) UUID subjectId,
                                     @RequestParam(required = false) UUID topicId,
                                     @RequestParam(required = false) String priority,
                                     @RequestParam(required = false) String status) {
        return reteachCardService.list(subjectId, topicId, priority, status);
    }

    @GetMapping("/{id}")
    public ReteachCardDetailDto get(@PathVariable UUID id) {
        return reteachCardService.getDetail(id);
    }

    @PutMapping("/{id}")
    public ReteachCardDto update(@PathVariable UUID id,
                                 @RequestBody UpdateReteachCardRequest request) {
        return reteachCardService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id) {
        reteachCardService.delete(id);
        return Map.of("message", "Re-teach card deleted");
    }
}

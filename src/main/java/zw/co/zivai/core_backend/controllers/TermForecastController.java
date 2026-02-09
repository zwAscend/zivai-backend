package zw.co.zivai.core_backend.controllers;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.TermForecastRequest;
import zw.co.zivai.core_backend.models.lms.TermForecast;
import zw.co.zivai.core_backend.services.TermForecastService;

@RestController
@RequestMapping("/api/term-forecasts")
@RequiredArgsConstructor
public class TermForecastController {
    private final TermForecastService termForecastService;

    @GetMapping
    public List<TermForecast> list(
        @RequestParam UUID subjectId,
        @RequestParam(required = false) String term
    ) {
        return termForecastService.listBySubject(subjectId, term);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermForecast create(@RequestBody TermForecastRequest request) {
        return termForecastService.create(request);
    }

    @PutMapping("/{id}")
    public TermForecast update(@PathVariable UUID id, @RequestBody TermForecastRequest request) {
        return termForecastService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        termForecastService.delete(id);
    }
}

package zw.co.zivai.core_backend.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CurriculumForecastDto;
import zw.co.zivai.core_backend.dtos.TermForecastDto;
import zw.co.zivai.core_backend.services.ReportService;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/curriculum")
    public CurriculumForecastDto curriculum(@RequestParam(required = false) UUID subjectId) {
        return reportService.getCurriculumForecast(subjectId);
    }

    @GetMapping("/term-forecast")
    public TermForecastDto termForecast(
        @RequestParam(required = false) UUID subjectId,
        @RequestParam(required = false) String term
    ) {
        return reportService.getTermForecast(subjectId, term);
    }
}

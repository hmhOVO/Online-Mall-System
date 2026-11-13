package com.example.controller;

import com.example.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/export")
public class PersonalExportController {

    private final ExportService exportService;

    public PersonalExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/personal")
    public void exportPersonalWeeklyReport(HttpServletResponse response) throws IOException {
        exportService.exportPersonalWeeklyReport(response);
    }
}

package com.apisentinel.statuspage;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusPageController {
    private final StatusPageService statusPageService;

    public StatusPageController(StatusPageService statusPageService) {
        this.statusPageService = statusPageService;
    }

    @GetMapping("/api/public/status/{slug}")
    public StatusPageDtos.StatusPageResponse get(@PathVariable String slug) {
        return statusPageService.get(slug);
    }

    @GetMapping("/status/{slug}")
    public ResponseEntity<String> html(@PathVariable String slug) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(statusPageService.renderHtml(statusPageService.get(slug)));
    }
}

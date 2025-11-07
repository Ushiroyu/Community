package com.community.common.controller;

import com.community.common.util.SimpleMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/metrics")
public class MetricsController {

    @GetMapping("/basic")
    public java.util.Map<String, Long> basic() {
        return SimpleMetrics.snapshot();
    }
}

package com.wrongnote.controller;

import com.wrongnote.dto.ApiResponse;
import com.wrongnote.dto.ErrorRateDTO;
import com.wrongnote.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 查询错题率趋势
     */
    @GetMapping("/error-rate")
    public ApiResponse<List<ErrorRateDTO>> errorRate(
            @RequestParam(value = "userId", defaultValue = "1") Long userId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        List<ErrorRateDTO> trend = statsService.getErrorRateTrend(userId, days);
        return ApiResponse.ok(trend);
    }
}

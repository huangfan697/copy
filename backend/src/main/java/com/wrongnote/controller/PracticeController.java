package com.wrongnote.controller;

import com.wrongnote.dto.AnswerSubmitDTO;
import com.wrongnote.dto.ApiResponse;
import com.wrongnote.entity.PracticeQuestion;
import com.wrongnote.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    /**
     * 从错题生成练习题
     */
    @PostMapping("/generate/{noteId}")
    public ApiResponse<List<PracticeQuestion>> generate(
            @PathVariable Long noteId,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        try {
            List<PracticeQuestion> questions = practiceService.generateQuestions(noteId, userId);
            return ApiResponse.ok(questions);
        } catch (Exception e) {
            return ApiResponse.fail("生成练习题失败: " + e.getMessage());
        }
    }

    /**
     * 获取错题的练习题列表
     */
    @GetMapping("/list/{noteId}")
    public ApiResponse<List<PracticeQuestion>> list(
            @PathVariable Long noteId,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        List<PracticeQuestion> questions = practiceService.listByNote(noteId, userId);
        return ApiResponse.ok(questions);
    }

    /**
     * 今日训练：获取昨日错题 + 昨日新导入的练习题
     */
    @GetMapping("/today")
    public ApiResponse<Map<String, Object>> todayTrain(
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        return ApiResponse.ok(practiceService.getTodayTrainInfo(userId));
    }

    /**
     * 今日训练：获取训练题目列表（昨日错题 + 昨日新导入）
     */
    @GetMapping("/today/questions")
    public ApiResponse<List<PracticeQuestion>> todayQuestions(
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        return ApiResponse.ok(practiceService.getTodayQuestions(userId));
    }

    /**
     * 提交答案
     */
    @PostMapping("/submit")
    public ApiResponse<PracticeService.AnswerResult> submit(
            @RequestBody AnswerSubmitDTO dto,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        try {
            PracticeService.AnswerResult result = practiceService.submitAnswer(dto, userId);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.fail("提交失败: " + e.getMessage());
        }
    }
}

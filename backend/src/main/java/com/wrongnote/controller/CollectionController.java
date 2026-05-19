package com.wrongnote.controller;

import com.wrongnote.dto.ApiResponse;
import com.wrongnote.entity.DailyCollection;
import com.wrongnote.entity.PracticeQuestion;
import com.wrongnote.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * 获取用户的每日栏目列表
     */
    @GetMapping("")
    public ApiResponse<List<DailyCollection>> list(
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        return ApiResponse.ok(collectionService.listByUser(userId));
    }

    /**
     * 获取栏目详情（含错题 + 练习题）
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable Long id,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        try {
            return ApiResponse.ok(collectionService.getCollectionDetail(id, userId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 获取栏目下的练习题（用于训练）
     */
    @GetMapping("/{id}/questions")
    public ApiResponse<List<PracticeQuestion>> questions(
            @PathVariable Long id,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        return ApiResponse.ok(collectionService.getCollectionQuestions(id, userId));
    }
}

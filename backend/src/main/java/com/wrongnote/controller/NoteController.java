package com.wrongnote.controller;

import com.wrongnote.dto.ApiResponse;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 上传错题图片（异步 AI 解析，立即返回）
     */
    @PostMapping("/upload")
    public ApiResponse<WrongNote> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        try {
            WrongNote note = noteService.uploadImage(file, userId);
            return ApiResponse.ok(note);
        } catch (Exception e) {
            return ApiResponse.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户错题列表
     */
    @GetMapping("")
    public ApiResponse<List<WrongNote>> list(
            @RequestParam(value = "userId", defaultValue = "1") Long userId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer status) {
        List<WrongNote> notes = noteService.listByUser(userId, subject, status);
        return ApiResponse.ok(notes);
    }

    /**
     * 查询错题详情
     */
    @GetMapping("/{id}")
    public ApiResponse<WrongNote> detail(@PathVariable Long id) {
        WrongNote note = noteService.getById(id);
        if (note == null) {
            return ApiResponse.fail("错题不存在");
        }
        return ApiResponse.ok(note);
    }

    /**
     * 更新掌握状态
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return ApiResponse.fail("status 必须为 0 或 1");
        }
        noteService.updateStatus(id, status);
        return ApiResponse.ok();
    }
}

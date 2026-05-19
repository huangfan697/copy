package com.wrongnote.controller;

import com.wrongnote.dto.ApiResponse;
import com.wrongnote.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 小程序登录：接收 code，返回 userId
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return ApiResponse.fail("code 不能为空");
        }
        try {
            Map<String, Object> result = authService.login(
                    code,
                    body.get("nickname"),
                    body.get("avatarUrl")
            );
            return ApiResponse.ok(result);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}

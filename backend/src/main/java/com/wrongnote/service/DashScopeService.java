package com.wrongnote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrongnote.config.DashScopeConfig;
import com.wrongnote.dto.NoteParseResult;
import com.wrongnote.dto.PracticeQuestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeService {

    private final DashScopeConfig dashScopeConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 解析错题图片：只提取被标记为错误的题目
     */
    public NoteParseResult parseWrongNote(String imageUrl) {
        String systemPrompt = "你是一位经验丰富的老师。请仔细分析图片中每一道题，只提取用户标记为错误的题目（例如：答案被划掉/划红线、旁边手写了正确答案、打了叉等）。"
                + "如果一张图包含多道错题，请返回数组格式；如果只有单道错题，返回单个 JSON 对象。\n"
                + "只提取错题，答对的题目不要返回。\n"
                + "每道题格式：{\"subject\":\"科目名称\",\"content\":\"题目完整文本，公式用LaTeX\",\"userAnswer\":\"用户写的答案（如有）\",\"correctAnswer\":\"红线标注的正确答案\",\"analysis\":\"解题思路和关键点分析\",\"tags\":[\"知识点1\",\"知识点2\"]}";

        String result = callVisionModel(systemPrompt, imageUrl);
        return parseJsonResponse(result, NoteParseResult.class);
    }

    /**
     * 根据错题生成练习题
     */
    public List<PracticeQuestionDTO> generatePractice(String rawContent, List<String> tags) {
        String tagsStr = tags != null ? String.join("、", tags) : "相关知识点";

        String systemPrompt = "你是一位经验丰富的老师。基于以下错题，生成 5 道同知识点的变式题（难度相近但数据不同）。"
                + "严格按照 JSON 数组格式返回，不要包含其他文字。\n"
                + "每道题格式：{\"questionText\":\"题目文本\",\"options\":{\"A\":\"选项A\",\"B\":\"选项B\",\"C\":\"选项C\",\"D\":\"选项D\"},\"answer\":\"正确答案字母\",\"explanation\":\"解析说明\"}";

        String userPrompt = "错题内容：" + rawContent + "\n知识点：" + tagsStr;

        String result = callTextModel(systemPrompt, userPrompt);
        List<PracticeQuestionDTO> questions = parseJsonArrayResponse(result, PracticeQuestionDTO.class);
        return questions.isEmpty() ? Collections.emptyList() : questions;
    }

    private String callVisionModel(String systemPrompt, String imageUrl) {
        List<Map<String, Object>> systemContent = List.of(Map.of("type", "text", "text", systemPrompt));
        List<Map<String, Object>> userContent = Arrays.asList(
                Map.of("type", "text", "text", "请识别这张图片中的错题，只返回被标记错误的题目"),
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
        );

        List<Map<String, Object>> messages = Arrays.asList(
                Map.of("role", "system", "content", systemContent),
                Map.of("role", "user", "content", userContent)
        );

        Map<String, Object> body = Map.of(
                "model", dashScopeConfig.getModel(),
                "messages", messages,
                "max_tokens", 4096
        );

        return postJson(body);
    }

    private String callTextModel(String systemPrompt, String userPrompt) {
        List<Map<String, Object>> systemContent = List.of(Map.of("type", "text", "text", systemPrompt));
        List<Map<String, Object>> userContent = List.of(Map.of("type", "text", "text", userPrompt));

        List<Map<String, Object>> messages = Arrays.asList(
                Map.of("role", "system", "content", systemContent),
                Map.of("role", "user", "content", userContent)
        );

        Map<String, Object> body = Map.of(
                "model", dashScopeConfig.getModel(),
                "messages", messages,
                "max_tokens", 4096
        );

        return postJson(body);
    }

    private String postJson(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(dashScopeConfig.getApiKey());

        String url = dashScopeConfig.getBaseUrl() + "/v1/chat/completions";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
            throw new RuntimeException("AI 返回结果中没有 choices");
        } catch (RestClientException e) {
            log.error("调用 AI 模型失败", e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    private <T> T parseJsonResponse(String response, Class<T> clazz) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("解析 AI JSON 响应失败: {}", response, e);
            throw new RuntimeException("AI 返回格式解析失败: " + e.getMessage(), e);
        }
    }

    private <T> List<T> parseJsonArrayResponse(String response, Class<T> clazz) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
            }
            return Collections.emptyList();
        } catch (JsonProcessingException e) {
            log.error("解析 AI JSON 数组响应失败: {}", response, e);
            return Collections.emptyList();
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf("```");
        if (start >= 0) {
            int jsonStart = response.indexOf('\n', start) + 1;
            int end = response.indexOf("```", jsonStart);
            if (end > jsonStart) {
                String inner = response.substring(jsonStart, end).trim();
                if (inner.startsWith("json")) {
                    inner = inner.substring(4).trim();
                }
                return inner;
            }
        }
        return response.trim();
    }
}

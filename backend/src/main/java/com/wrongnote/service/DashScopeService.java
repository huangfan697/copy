package com.wrongnote.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.wrongnote.config.DashScopeConfig;
import com.wrongnote.dto.NoteParseResult;
import com.wrongnote.dto.PracticeQuestionDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeService {

    private final DashScopeConfig dashScopeConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析错题图片：调用通义千问 VL 模型识别图片中的题目
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
        MultiModalConversation conv = new MultiModalConversation();

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("image", imageUrl);

        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", systemPrompt)))
                .build();

        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("text", "请识别这张图片中的题目"),
                        imageContent
                ))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(dashScopeConfig.getApiKey())
                .model(dashScopeConfig.getModel())
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        try {
            MultiModalConversationResult result = conv.call(param);
            return extractTextFromResult(result);
        } catch (Exception e) {
            log.error("调用通义千问 VL 失败", e);
            throw new RuntimeException("AI 解析失败: " + e.getMessage(), e);
        }
    }

    private String callTextModel(String systemPrompt, String userPrompt) {
        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", systemPrompt)))
                .build();

        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", userPrompt)))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(dashScopeConfig.getApiKey())
                .model(dashScopeConfig.getModel())
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        try {
            MultiModalConversationResult result = conv.call(param);
            return extractTextFromResult(result);
        } catch (Exception e) {
            log.error("调用通义千问生成题目失败", e);
            throw new RuntimeException("AI 生成题目失败: " + e.getMessage(), e);
        }
    }

    private String extractTextFromResult(MultiModalConversationResult result) {
        if (result == null || result.getOutput() == null) {
            throw new RuntimeException("AI 返回结果为空");
        }

        MultiModalConversationOutput output = result.getOutput();
        if (output.getChoices() == null || output.getChoices().isEmpty()) {
            throw new RuntimeException("AI 返回结果中没有 choices");
        }

        var choice = output.getChoices().get(0);
        if (choice.getMessage() == null || choice.getMessage().getContent() == null) {
            throw new RuntimeException("AI 返回结果中没有 message");
        }

        var content = choice.getMessage().getContent();
        for (Object item : content) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                if (map.containsKey("text")) {
                    Object text = map.get("text");
                    if (text != null) {
                        return text.toString();
                    }
                }
            }
        }

        // Fallback
        for (Object item : content) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                for (Object value : map.values()) {
                    if (value instanceof String s && !s.isEmpty()) {
                        return s;
                    }
                }
            }
        }

        throw new RuntimeException("无法从 AI 返回结果中提取文本");
    }

    private <T> T parseJsonResponse(String response, Class<T> clazz) {
        try {
            // 尝试从 markdown code block 中提取 JSON
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
        // 处理 markdown code block: ```json ... ```
        int start = response.indexOf("```");
        if (start >= 0) {
            int jsonStart = response.indexOf('\n', start) + 1;
            int end = response.indexOf("```", jsonStart);
            if (end > jsonStart) {
                String inner = response.substring(jsonStart, end).trim();
                // 去掉可能的 "json" 前缀
                if (inner.startsWith("json")) {
                    inner = inner.substring(4).trim();
                }
                return inner;
            }
        }
        return response.trim();
    }
}

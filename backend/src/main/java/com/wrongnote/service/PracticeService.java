package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrongnote.dto.AnswerSubmitDTO;
import com.wrongnote.dto.PracticeQuestionDTO;
import com.wrongnote.entity.PracticeQuestion;
import com.wrongnote.entity.PracticeRecord;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.mapper.PracticeQuestionMapper;
import com.wrongnote.mapper.PracticeRecordMapper;
import com.wrongnote.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeQuestionMapper practiceQuestionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final WrongNoteMapper wrongNoteMapper;
    private final DashScopeService dashScopeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据错题生成练习题
     */
    @Transactional
    public List<PracticeQuestion> generateQuestions(Long noteId, Long userId) {
        WrongNote note = wrongNoteMapper.selectById(noteId);
        if (note == null) {
            throw new IllegalArgumentException("错题不存在: " + noteId);
        }

        List<String> tags = parseTags(note.getKnowledgeTags());
        List<PracticeQuestionDTO> dtos = dashScopeService.generatePractice(note.getRawContent(), tags);

        List<PracticeQuestion> questions = new ArrayList<>();
        for (PracticeQuestionDTO dto : dtos) {
            PracticeQuestion q = new PracticeQuestion();
            q.setSourceNoteId(noteId);
            q.setUserId(userId);
            q.setQuestionText(dto.getQuestionText());
            try {
                q.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (JsonProcessingException e) {
                q.setOptions("{}");
            }
            q.setAnswer(dto.getAnswer());
            q.setExplanation(dto.getExplanation());
            q.setIsCorrect(null);

            practiceQuestionMapper.insert(q);
            questions.add(q);
        }

        log.info("生成 {} 道练习题, noteId={}", questions.size(), noteId);
        return questions;
    }

    /**
     * 获取练习题列表
     */
    public List<PracticeQuestion> listByNote(Long noteId, Long userId) {
        LambdaQueryWrapper<PracticeQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PracticeQuestion::getSourceNoteId, noteId)
                .eq(PracticeQuestion::getUserId, userId)
                .orderByAsc(PracticeQuestion::getCreatedAt);
        return practiceQuestionMapper.selectList(wrapper);
    }

    /**
     * 提交答案并批改
     */
    @Transactional
    public AnswerResult submitAnswer(AnswerSubmitDTO dto, Long userId) {
        PracticeQuestion q = practiceQuestionMapper.selectById(dto.getQuestionId());
        if (q == null) {
            throw new IllegalArgumentException("题目不存在: " + dto.getQuestionId());
        }

        boolean correct = dto.getUserAnswer() != null
                && dto.getUserAnswer().equalsIgnoreCase(q.getAnswer());
        q.setIsCorrect(correct ? 1 : 0);
        practiceQuestionMapper.updateById(q);

        // 更新每日统计
        updateDailyRecord(userId);

        return new AnswerResult(q.getId(), correct, q.getAnswer(), q.getExplanation());
    }

    /**
     * 更新每日答题统计
     */
    private void updateDailyRecord(Long userId) {
        LocalDate today = LocalDate.now();

        // 查询当日正确数量
        LambdaQueryWrapper<PracticeQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PracticeQuestion::getUserId, userId)
                .ge(PracticeQuestion::getCreatedAt, today.atStartOfDay())
                .isNotNull(PracticeQuestion::getIsCorrect);

        List<PracticeQuestion> answered = practiceQuestionMapper.selectList(wrapper);
        int total = answered.size();
        int correct = (int) answered.stream().filter(q -> q.getIsCorrect() == 1).count();

        BigDecimal errorRate = total > 0
                ? BigDecimal.valueOf(total - correct)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 查询是否已有记录
        LambdaQueryWrapper<PracticeRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getPracticeDate, today);

        PracticeRecord record = practiceRecordMapper.selectOne(recordWrapper);
        if (record == null) {
            record = new PracticeRecord();
            record.setUserId(userId);
            record.setPracticeDate(today);
            record.setTotalCount(total);
            record.setCorrectCount(correct);
            record.setErrorRate(errorRate);
            practiceRecordMapper.insert(record);
        } else {
            record.setTotalCount(total);
            record.setCorrectCount(correct);
            record.setErrorRate(errorRate);
            practiceRecordMapper.updateById(record);
        }
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public record AnswerResult(Long questionId, boolean correct, String correctAnswer, String explanation) {
    }

    /**
     * 获取今日训练信息：昨日答错的题 + 昨日新导入的未答题
     */
    public Map<String, Object> getTodayTrainInfo(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate yesterdayEnd = yesterday.plusDays(1);

        // 1. 昨天答错的题目（updated_at 在昨天范围内）
        LambdaQueryWrapper<PracticeQuestion> wrongWrapper = new LambdaQueryWrapper<>();
        wrongWrapper.eq(PracticeQuestion::getUserId, userId)
                .eq(PracticeQuestion::getIsCorrect, 0)
                .ge(PracticeQuestion::getUpdatedAt, yesterday.atStartOfDay())
                .lt(PracticeQuestion::getUpdatedAt, yesterdayEnd.atStartOfDay());
        long yesterdayWrong = practiceQuestionMapper.selectCount(wrongWrapper);

        // 2. 昨天新导入的笔记下的练习题（created_at 在昨天范围内）
        LambdaQueryWrapper<PracticeQuestion> newWrapper = new LambdaQueryWrapper<>();
        newWrapper.eq(PracticeQuestion::getUserId, userId)
                .isNull(PracticeQuestion::getIsCorrect)
                .ge(PracticeQuestion::getCreatedAt, yesterday.atStartOfDay())
                .lt(PracticeQuestion::getCreatedAt, yesterdayEnd.atStartOfDay());
        long yesterdayNew = practiceQuestionMapper.selectCount(newWrapper);

        return Map.of(
                "yesterdayWrong", yesterdayWrong,
                "yesterdayNew", yesterdayNew,
                "totalCount", yesterdayWrong + yesterdayNew
        );
    }

    /**
     * 获取今日训练题目列表
     */
    public List<PracticeQuestion> getTodayQuestions(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate yesterdayEnd = yesterday.plusDays(1);

        // 昨天答错的
        LambdaQueryWrapper<PracticeQuestion> wrongWrapper = new LambdaQueryWrapper<>();
        wrongWrapper.eq(PracticeQuestion::getUserId, userId)
                .eq(PracticeQuestion::getIsCorrect, 0)
                .ge(PracticeQuestion::getUpdatedAt, yesterday.atStartOfDay())
                .lt(PracticeQuestion::getUpdatedAt, yesterdayEnd.atStartOfDay())
                .orderByAsc(PracticeQuestion::getId);
        List<PracticeQuestion> wrongQuestions = practiceQuestionMapper.selectList(wrongWrapper);

        // 昨天新导入未答的
        LambdaQueryWrapper<PracticeQuestion> newWrapper = new LambdaQueryWrapper<>();
        newWrapper.eq(PracticeQuestion::getUserId, userId)
                .isNull(PracticeQuestion::getIsCorrect)
                .ge(PracticeQuestion::getCreatedAt, yesterday.atStartOfDay())
                .lt(PracticeQuestion::getCreatedAt, yesterdayEnd.atStartOfDay())
                .orderByAsc(PracticeQuestion::getId);
        List<PracticeQuestion> newQuestions = practiceQuestionMapper.selectList(newWrapper);

        // 合并去重（同一个题可能既在昨天新导入又被答错）
        List<PracticeQuestion> all = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (PracticeQuestion q : wrongQuestions) {
            if (seen.add(q.getId())) all.add(q);
        }
        for (PracticeQuestion q : newQuestions) {
            if (seen.add(q.getId())) all.add(q);
        }
        return all;
    }
}

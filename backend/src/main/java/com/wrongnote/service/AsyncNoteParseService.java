package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrongnote.dto.NoteParseResult;
import com.wrongnote.entity.DailyCollection;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.mapper.DailyCollectionMapper;
import com.wrongnote.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncNoteParseService {

    private final WrongNoteMapper wrongNoteMapper;
    private final DailyCollectionMapper dailyCollectionMapper;
    private final DashScopeService dashScopeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 异步 AI 解析并保存错题
     * 必须放在独立 Service 中，避免 self-invocation 绕过 Spring AOP 代理
     */
    @Async
    public void parseAndSaveAsync(Long noteId, Long userId) {
        WrongNote note = wrongNoteMapper.selectById(noteId);
        if (note == null) {
            log.warn("异步解析时 note 不存在: {}", noteId);
            return;
        }

        try {
            List<NoteParseResult> results = dashScopeService.parseWrongNote(note.getImageUrl());
            log.info("AI 解析结果: noteId={}, 共 {} 道错题", noteId, results.size());

            if (results.isEmpty()) {
                note.setStatus(0);
                note.setSubject("未识别到错题");
                note.setRawContent("未检测到红线标记的错题，请确保图片中有明确的错题标记");
                wrongNoteMapper.updateById(note);
                return;
            }

            // 更新第一条为解析结果
            NoteParseResult first = results.get(0);
            note.setSubject(first.getSubject());
            note.setRawContent(first.getContent());
            note.setCorrectAnswer(first.getCorrectAnswer());
            note.setAnalysis(first.getAnalysis());
            try {
                note.setKnowledgeTags(objectMapper.writeValueAsString(first.getTags()));
            } catch (JsonProcessingException e) {
                note.setKnowledgeTags("[]");
            }
            note.setStatus(0);
            wrongNoteMapper.updateById(note);

            // 多余错题另存为独立记录
            for (int i = 1; i < results.size(); i++) {
                NoteParseResult r = results.get(i);
                WrongNote extra = new WrongNote();
                extra.setUserId(userId);
                extra.setImageUrl(note.getImageUrl());
                extra.setSubject(r.getSubject());
                extra.setRawContent(r.getContent());
                extra.setCorrectAnswer(r.getCorrectAnswer());
                extra.setAnalysis(r.getAnalysis());
                try {
                    extra.setKnowledgeTags(objectMapper.writeValueAsString(r.getTags()));
                } catch (JsonProcessingException e) {
                    extra.setKnowledgeTags("[]");
                }
                extra.setStatus(0);
                extra.setCollectionId(note.getCollectionId());
                wrongNoteMapper.insert(extra);
            }

            // 更新栏目计数
            int totalNotes = results.size();
            LambdaUpdateWrapper<DailyCollection> updateWrapper =
                    new LambdaUpdateWrapper<>();
            updateWrapper.eq(DailyCollection::getId, note.getCollectionId())
                    .setSql("note_count = note_count + " + totalNotes);
            dailyCollectionMapper.update(null, updateWrapper);

            log.info("AI 解析完成, noteId={}, saved {} notes", noteId, totalNotes);
        } catch (Exception e) {
            log.error("AI 解析失败, noteId={}", noteId, e);
            note.setStatus(-2);
            note.setSubject("解析失败");
            note.setRawContent("AI 解析出错: " + e.getMessage());
            wrongNoteMapper.updateById(note);
        }
    }
}

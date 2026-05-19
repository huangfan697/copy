package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrongnote.dto.ApiResponse;
import com.wrongnote.dto.NoteParseResult;
import com.wrongnote.entity.DailyCollection;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.mapper.DailyCollectionMapper;
import com.wrongnote.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final WrongNoteMapper wrongNoteMapper;
    private final DailyCollectionMapper dailyCollectionMapper;
    private final OssService ossService;
    private final DashScopeService dashScopeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传错题图片：只保存图片，立即返回，AI 解析异步执行
     * 返回的 note status = -1 表示解析中
     */
    public WrongNote uploadImage(MultipartFile file, Long userId) throws Exception {
        String imageUrl = ossService.uploadImage(file);
        log.info("图片上传成功: {}", imageUrl);

        // 关联到今日每日栏目
        Long collectionId = getOrCreateTodayCollection(userId);

        // 创建一条 status=-1 的待解析记录
        WrongNote note = new WrongNote();
        note.setUserId(userId);
        note.setImageUrl(imageUrl);
        note.setStatus(-1); // -1 = 解析中
        note.setCollectionId(collectionId);
        note.setSubject("解析中...");
        note.setRawContent("");
        wrongNoteMapper.insert(note);
        log.info("待解析记录创建成功, id={}", note.getId());

        // 异步触发 AI 解析
        parseAndSaveAsync(note.getId(), userId);

        return note;
    }

    /**
     * 异步 AI 解析并保存错题
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
                // 没识别到错题，更新为提示
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
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DailyCollection> updateWrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            updateWrapper.eq(DailyCollection::getId, note.getCollectionId())
                    .setSql("note_count = note_count + " + totalNotes);
            dailyCollectionMapper.update(null, updateWrapper);

            log.info("AI 解析完成, noteId={}, saved {} notes", noteId, totalNotes);
        } catch (Exception e) {
            log.error("AI 解析失败, noteId={}", noteId, e);
            note.setStatus(-2); // -2 = 解析失败
            note.setSubject("解析失败");
            note.setRawContent("AI 解析出错: " + e.getMessage());
            wrongNoteMapper.updateById(note);
        }
    }

    /**
     * 获取或创建今日的 daily_collection
     */
    private Long getOrCreateTodayCollection(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<DailyCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCollection::getUserId, userId)
                .eq(DailyCollection::getCollectionDate, today);
        DailyCollection existing = dailyCollectionMapper.selectOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }
        DailyCollection collection = new DailyCollection();
        collection.setUserId(userId);
        collection.setCollectionDate(today);
        collection.setNoteCount(0);
        collection.setQuestionCount(0);
        dailyCollectionMapper.insert(collection);
        return collection.getId();
    }

    /**
     * 查询用户错题列表
     */
    public List<WrongNote> listByUser(Long userId, String subject, Integer status) {
        LambdaQueryWrapper<WrongNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongNote::getUserId, userId);
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(WrongNote::getSubject, subject);
        }
        if (status != null) {
            wrapper.eq(WrongNote::getStatus, status);
        }
        wrapper.orderByDesc(WrongNote::getCreatedAt);
        return wrongNoteMapper.selectList(wrapper);
    }

    /**
     * 查询错题详情
     */
    public WrongNote getById(Long id) {
        return wrongNoteMapper.selectById(id);
    }

    /**
     * 更新掌握状态
     */
    public void updateStatus(Long id, Integer status) {
        WrongNote note = wrongNoteMapper.selectById(id);
        if (note != null) {
            note.setStatus(status);
            wrongNoteMapper.updateById(note);
        }
    }
}

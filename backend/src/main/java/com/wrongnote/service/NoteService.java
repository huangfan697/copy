package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrongnote.dto.ApiResponse;
import com.wrongnote.dto.NoteParseResult;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final WrongNoteMapper wrongNoteMapper;
    private final OssService ossService;
    private final DashScopeService dashScopeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传错题图片并自动解析
     */
    @Transactional
    public WrongNote uploadAndParse(MultipartFile file, Long userId) throws Exception {
        // 1. 上传图片到 OSS
        String imageUrl = ossService.uploadImage(file);
        log.info("图片上传成功: {}", imageUrl);

        // 2. 调用 AI 解析
        NoteParseResult result = dashScopeService.parseWrongNote(imageUrl);
        log.info("AI 解析结果: subject={}, tags={}", result.getSubject(), result.getTags());

        // 3. 保存到数据库
        WrongNote note = new WrongNote();
        note.setUserId(userId);
        note.setImageUrl(imageUrl);
        note.setSubject(result.getSubject());
        note.setRawContent(result.getContent());
        note.setAnalysis(result.getAnalysis());
        try {
            note.setKnowledgeTags(objectMapper.writeValueAsString(result.getTags()));
        } catch (JsonProcessingException e) {
            note.setKnowledgeTags("[]");
        }
        note.setStatus(0);

        wrongNoteMapper.insert(note);
        log.info("错题笔记保存成功, id={}", note.getId());

        return note;
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

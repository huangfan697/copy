package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrongnote.entity.DailyCollection;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.mapper.DailyCollectionMapper;
import com.wrongnote.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AsyncNoteParseService asyncNoteParseService;

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

        // 异步触发 AI 解析（通过独立 Service 走 Spring AOP 代理）
        asyncNoteParseService.parseAndSaveAsync(note.getId(), userId);

        return note;
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
        // 列表查询排除 imageUrl，避免传输大量 base64 图片数据
        wrapper.select(WrongNote.class, info -> !"image_url".equals(info.getColumn()));
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

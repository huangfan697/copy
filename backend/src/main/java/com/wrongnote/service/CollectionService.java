package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrongnote.entity.DailyCollection;
import com.wrongnote.entity.PracticeQuestion;
import com.wrongnote.entity.WrongNote;
import com.wrongnote.mapper.DailyCollectionMapper;
import com.wrongnote.mapper.PracticeQuestionMapper;
import com.wrongnote.mapper.WrongNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final DailyCollectionMapper dailyCollectionMapper;
    private final WrongNoteMapper wrongNoteMapper;
    private final PracticeQuestionMapper practiceQuestionMapper;

    /**
     * 获取用户的每日栏目列表（按日期倒序）
     */
    public List<DailyCollection> listByUser(Long userId) {
        LambdaQueryWrapper<DailyCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCollection::getUserId, userId)
                .orderByDesc(DailyCollection::getCollectionDate)
                .last("LIMIT 30");
        return dailyCollectionMapper.selectList(wrapper);
    }

    /**
     * 获取栏目详情（包含该栏目下的错题和练习题）
     */
    public Map<String, Object> getCollectionDetail(Long collectionId, Long userId) {
        DailyCollection collection = dailyCollectionMapper.selectById(collectionId);
        if (collection == null) {
            throw new IllegalArgumentException("栏目不存在: " + collectionId);
        }

        // 查询该栏目下的错题（排除 imageUrl，避免传输大量 base64 数据）
        LambdaQueryWrapper<WrongNote> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.select(WrongNote.class, info -> !"image_url".equals(info.getColumn()))
                .eq(WrongNote::getCollectionId, collectionId)
                .eq(WrongNote::getUserId, userId)
                .orderByDesc(WrongNote::getCreatedAt);
        List<WrongNote> notes = wrongNoteMapper.selectList(noteWrapper);

        // 查询该栏目下错题对应的练习题
        List<Long> noteIds = notes.stream().map(WrongNote::getId).toList();
        List<PracticeQuestion> questions = List.of();
        if (!noteIds.isEmpty()) {
            LambdaQueryWrapper<PracticeQuestion> qWrapper = new LambdaQueryWrapper<>();
            qWrapper.in(PracticeQuestion::getSourceNoteId, noteIds)
                    .eq(PracticeQuestion::getUserId, userId)
                    .orderByAsc(PracticeQuestion::getId);
            questions = practiceQuestionMapper.selectList(qWrapper);
        }

        return Map.of(
                "collection", collection,
                "notes", notes,
                "questions", questions
        );
    }

    /**
     * 获取栏目下的练习题（用于训练）
     */
    public List<PracticeQuestion> getCollectionQuestions(Long collectionId, Long userId) {
        // 先查该栏目下的错题ID（只需要 ID，不需要其他字段）
        LambdaQueryWrapper<WrongNote> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.select(WrongNote::getId)
                .eq(WrongNote::getCollectionId, collectionId)
                .eq(WrongNote::getUserId, userId);
        List<WrongNote> notes = wrongNoteMapper.selectList(noteWrapper);

        List<Long> noteIds = notes.stream().map(WrongNote::getId).toList();
        if (noteIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<PracticeQuestion> qWrapper = new LambdaQueryWrapper<>();
        qWrapper.in(PracticeQuestion::getSourceNoteId, noteIds)
                .eq(PracticeQuestion::getUserId, userId)
                .orderByAsc(PracticeQuestion::getId);
        return practiceQuestionMapper.selectList(qWrapper);
    }

    /**
     * 获取昨日栏目（用于今日训练）
     */
    public DailyCollection getYesterdayCollection(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LambdaQueryWrapper<DailyCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCollection::getUserId, userId)
                .eq(DailyCollection::getCollectionDate, yesterday);
        return dailyCollectionMapper.selectOne(wrapper);
    }
}

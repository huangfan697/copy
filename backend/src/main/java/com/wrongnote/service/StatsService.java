package com.wrongnote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrongnote.dto.ErrorRateDTO;
import com.wrongnote.entity.PracticeRecord;
import com.wrongnote.mapper.PracticeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PracticeRecordMapper practiceRecordMapper;

    /**
     * 查询 N 天错题率趋势
     */
    public List<ErrorRateDTO> getErrorRateTrend(Long userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        LambdaQueryWrapper<PracticeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PracticeRecord::getUserId, userId)
                .ge(PracticeRecord::getPracticeDate, startDate)
                .orderByAsc(PracticeRecord::getPracticeDate);

        List<PracticeRecord> records = practiceRecordMapper.selectList(wrapper);

        // 转换为 DTO，补齐没有答题的日期
        Map<LocalDate, PracticeRecord> recordMap = records.stream()
                .collect(Collectors.toMap(PracticeRecord::getPracticeDate, r -> r));

        List<ErrorRateDTO> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            ErrorRateDTO dto = new ErrorRateDTO();
            dto.setDate(date);

            PracticeRecord record = recordMap.get(date);
            if (record != null) {
                dto.setTotalCount(record.getTotalCount());
                dto.setCorrectCount(record.getCorrectCount());
                dto.setErrorRate(record.getErrorRate());
            } else {
                dto.setTotalCount(0);
                dto.setCorrectCount(0);
                dto.setErrorRate(java.math.BigDecimal.ZERO);
            }
            result.add(dto);
        }

        return result;
    }
}

package com.wrongnote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate practiceDate;

    private Integer totalCount;

    private Integer correctCount;

    private BigDecimal errorRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

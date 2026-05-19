package com.wrongnote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_collection")
public class DailyCollection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate collectionDate;

    private Integer noteCount;

    private Integer questionCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

package com.wrongnote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("practice_question")
public class PracticeQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceNoteId;

    private Long userId;

    private String questionText;

    private String options;

    private String answer;

    private String explanation;

    private Integer isCorrect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

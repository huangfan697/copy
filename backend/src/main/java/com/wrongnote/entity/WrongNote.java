package com.wrongnote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_note")
public class WrongNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String imageUrl;

    private String subject;

    private String knowledgeTags;

    private String rawContent;

    private String correctAnswer;

    private String analysis;

    private Integer status;

    private Long collectionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

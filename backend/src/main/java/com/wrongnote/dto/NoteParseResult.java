package com.wrongnote.dto;

import lombok.Data;

import java.util.List;

@Data
public class NoteParseResult {

    private String subject;
    private String content;
    private String analysis;
    private List<String> tags;
}

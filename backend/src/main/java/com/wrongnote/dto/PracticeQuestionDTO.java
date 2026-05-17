package com.wrongnote.dto;

import lombok.Data;

@Data
public class PracticeQuestionDTO {

    private String questionText;
    private java.util.Map<String, String> options;
    private String answer;
    private String explanation;
}

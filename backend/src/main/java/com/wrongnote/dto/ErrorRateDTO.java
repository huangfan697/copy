package com.wrongnote.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ErrorRateDTO {

    private LocalDate date;
    private Integer totalCount;
    private Integer correctCount;
    private BigDecimal errorRate;
}

package com.example.bankcards.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private String message;
    private Long fromCardId;
    private Long toCardId;
    private BigDecimal amount;
    private BigDecimal fromCardNewBalance;
    private BigDecimal toCardNewBalance;
}

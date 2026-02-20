package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    private Long id;
    private String maskedCardNumber;
    private Long ownerId;
    private String ownerEmail;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CardResponse fromEntity(Card card, String maskedNumber) {
        return CardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(maskedNumber)
                .ownerId(card.getOwner().getId())
                .ownerEmail(card.getOwner().getEmail())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}

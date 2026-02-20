package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecification {

    private CardSpecification() {
    }

    public static Specification<Card> hasOwner(Long ownerId) {
        return (root, query, criteriaBuilder) -> {
            if (ownerId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
        };
    }

    public static Specification<Card> hasStatus(CardStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Card> cardNumberContains(String cardNumberMask) {
        return (root, query, criteriaBuilder) -> {
            if (cardNumberMask == null || cardNumberMask.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(root.get("cardNumber"), "%" + cardNumberMask + "%");
        };
    }
}

package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardSpecification;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;
    private final EncryptionUtil encryptionUtil;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = userService.getUserEntityById(request.getOwnerId());

        String cardNumber = encryptionUtil.generateCardNumber();
        String encryptedNumber = encryptionUtil.encrypt(cardNumber);

        Card card = Card.builder()
                .cardNumber(encryptedNumber)
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .build();

        card = cardRepository.save(card);

        String maskedNumber = encryptionUtil.maskCardNumber(cardNumber);
        return CardResponse.fromEntity(card, maskedNumber);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCurrentUserCards(CardStatus status, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        Specification<Card> spec = Specification
                .where(CardSpecification.hasOwner(currentUser.getId()))
                .and(CardSpecification.hasStatus(status));

        return cardRepository.findAll(spec, pageable)
                .map(this::toCardResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(CardStatus status, Pageable pageable) {
        Specification<Card> spec = Specification.where(CardSpecification.hasStatus(status));

        return cardRepository.findAll(spec, pageable)
                .map(this::toCardResponse);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id) {
        Card card = findCardById(id);
        User currentUser = userService.getCurrentUser();

        if (!isAdmin(currentUser) && !card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have access to this card");
        }

        return toCardResponse(card);
    }

    @Transactional
    public CardResponse blockCard(Long id) {
        Card card = findCardById(id);

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BadRequestException("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        card = cardRepository.save(card);

        return toCardResponse(card);
    }

    @Transactional
    public CardResponse activateCard(Long id) {
        Card card = findCardById(id);

        if (card.isExpired()) {
            throw new BadRequestException("Cannot activate an expired card");
        }

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new BadRequestException("Card is already active");
        }

        card.setStatus(CardStatus.ACTIVE);
        card = cardRepository.save(card);

        return toCardResponse(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = findCardById(id);
        cardRepository.delete(card);
    }

    @Transactional
    public CardResponse requestBlockCard(Long id) {
        User currentUser = userService.getCurrentUser();
        Card card = findCardById(id);

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only request to block your own cards");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BadRequestException("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        card = cardRepository.save(card);

        return toCardResponse(card);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        User currentUser = userService.getCurrentUser();

        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new BadRequestException("Cannot transfer to the same card");
        }

        Card fromCard = findCardById(request.getFromCardId());
        Card toCard = findCardById(request.getToCardId());

        if (!fromCard.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only transfer from your own cards");
        }

        if (!toCard.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only transfer to your own cards");
        }

        validateCardForTransfer(fromCard, "Source");
        validateCardForTransfer(toCard, "Destination");

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on the source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        return TransferResponse.builder()
                .message("Transfer completed successfully")
                .fromCardId(fromCard.getId())
                .toCardId(toCard.getId())
                .amount(request.getAmount())
                .fromCardNewBalance(fromCard.getBalance())
                .toCardNewBalance(toCard.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long cardId) {
        User currentUser = userService.getCurrentUser();
        Card card = findCardById(cardId);

        if (!isAdmin(currentUser) && !card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have access to this card");
        }

        String decryptedNumber = encryptionUtil.decrypt(card.getCardNumber());
        String maskedNumber = encryptionUtil.maskCardNumber(decryptedNumber);

        return BalanceResponse.builder()
                .cardId(card.getId())
                .maskedCardNumber(maskedNumber)
                .balance(card.getBalance())
                .build();
    }

    @Transactional
    public void updateExpiredCards() {
        cardRepository.findAll().forEach(card -> {
            if (card.getExpiryDate().isBefore(LocalDate.now()) && card.getStatus() != CardStatus.EXPIRED) {
                card.setStatus(CardStatus.EXPIRED);
                cardRepository.save(card);
            }
        });
    }

    private Card findCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", id));
    }

    private CardResponse toCardResponse(Card card) {
        String decryptedNumber = encryptionUtil.decrypt(card.getCardNumber());
        String maskedNumber = encryptionUtil.maskCardNumber(decryptedNumber);
        return CardResponse.fromEntity(card, maskedNumber);
    }

    private void validateCardForTransfer(Card card, String cardType) {
        if (!card.isActive()) {
            if (card.isExpired()) {
                throw new CardNotActiveException(cardType + " card has expired");
            }
            throw new CardNotActiveException(cardType + " card is blocked");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRole().name().equals("ADMIN");
    }
}

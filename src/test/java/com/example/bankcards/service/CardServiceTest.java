package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted_card_number")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void createCard_Success() {
        CreateCardRequest request = new CreateCardRequest(
                1L,
                LocalDate.now().plusYears(3),
                new BigDecimal("500.00")
        );

        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(encryptionUtil.generateCardNumber()).thenReturn("4111111111111111");
        when(encryptionUtil.encrypt("4111111111111111")).thenReturn("encrypted");
        when(encryptionUtil.maskCardNumber("4111111111111111")).thenReturn("**** **** **** 1111");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(1L);
            return card;
        });

        CardResponse response = cardService.createCard(request);

        assertNotNull(response);
        assertEquals("**** **** **** 1111", response.getMaskedCardNumber());
        assertEquals(1L, response.getOwnerId());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void blockCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(encryptionUtil.decrypt("encrypted_card_number")).thenReturn("4111111111111111");
        when(encryptionUtil.maskCardNumber("4111111111111111")).thenReturn("**** **** **** 1111");

        CardResponse response = cardService.blockCard(1L);

        assertNotNull(response);
        assertEquals(CardStatus.BLOCKED, testCard.getStatus());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void blockCard_AlreadyBlocked_ThrowsException() {
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(BadRequestException.class, () -> cardService.blockCard(1L));
    }

    @Test
    void activateCard_Success() {
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(encryptionUtil.decrypt("encrypted_card_number")).thenReturn("4111111111111111");
        when(encryptionUtil.maskCardNumber("4111111111111111")).thenReturn("**** **** **** 1111");

        CardResponse response = cardService.activateCard(1L);

        assertNotNull(response);
        assertEquals(CardStatus.ACTIVE, testCard.getStatus());
    }

    @Test
    void activateCard_Expired_ThrowsException() {
        testCard.setStatus(CardStatus.BLOCKED);
        testCard.setExpiryDate(LocalDate.now().minusDays(1));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(BadRequestException.class, () -> cardService.activateCard(1L));
    }

    @Test
    void transfer_Success() {
        Card fromCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted1")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .cardNumber("encrypted2")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = cardService.transfer(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("800.00"), response.getFromCardNewBalance());
        assertEquals(new BigDecimal("700.00"), response.getToCardNewBalance());
    }

    @Test
    void transfer_InsufficientFunds_ThrowsException() {
        Card fromCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted1")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .cardNumber("encrypted2")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InsufficientFundsException.class, () -> cardService.transfer(request));
    }

    @Test
    void transfer_BlockedCard_ThrowsException() {
        Card fromCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted1")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("1000.00"))
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .cardNumber("encrypted2")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(CardNotActiveException.class, () -> cardService.transfer(request));
    }

    @Test
    void transfer_SameCard_ThrowsException() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("200.00"));

        when(userService.getCurrentUser()).thenReturn(testUser);

        assertThrows(BadRequestException.class, () -> cardService.transfer(request));
    }

    @Test
    void getCardById_NotFound_ThrowsException() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.getCardById(999L));
    }

    @Test
    void deleteCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        doNothing().when(cardRepository).delete(testCard);

        cardService.deleteCard(1L);

        verify(cardRepository, times(1)).delete(testCard);
    }
}

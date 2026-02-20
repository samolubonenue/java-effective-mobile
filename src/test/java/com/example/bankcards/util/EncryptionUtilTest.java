package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "secretKey", "TestAES256SecretKey32BytesLong!!");
        encryptionUtil.init();
    }

    @Test
    void encrypt_Decrypt_Success() {
        String originalText = "4111111111111111";

        String encrypted = encryptionUtil.encrypt(originalText);
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);

        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void encrypt_DifferentResults_ForSameInput() {
        String originalText = "4111111111111111";

        String encrypted1 = encryptionUtil.encrypt(originalText);
        String encrypted2 = encryptionUtil.encrypt(originalText);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void maskCardNumber_Success() {
        String cardNumber = "4111111111111234";

        String masked = encryptionUtil.maskCardNumber(cardNumber);

        assertEquals("**** **** **** 1234", masked);
    }

    @Test
    void maskCardNumber_ShortNumber() {
        String shortNumber = "123";

        String masked = encryptionUtil.maskCardNumber(shortNumber);

        assertEquals("****", masked);
    }

    @Test
    void maskCardNumber_Null() {
        String masked = encryptionUtil.maskCardNumber(null);

        assertEquals("****", masked);
    }

    @Test
    void generateCardNumber_ValidLength() {
        String cardNumber = encryptionUtil.generateCardNumber();

        assertNotNull(cardNumber);
        assertEquals(16, cardNumber.length());
    }

    @Test
    void generateCardNumber_StartsWithFour() {
        String cardNumber = encryptionUtil.generateCardNumber();

        assertTrue(cardNumber.startsWith("4"));
    }

    @Test
    void generateCardNumber_PassesLuhnCheck() {
        String cardNumber = encryptionUtil.generateCardNumber();

        assertTrue(isValidLuhn(cardNumber));
    }

    @Test
    void generateCardNumber_UniqueNumbers() {
        String cardNumber1 = encryptionUtil.generateCardNumber();
        String cardNumber2 = encryptionUtil.generateCardNumber();

        assertNotEquals(cardNumber1, cardNumber2);
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}

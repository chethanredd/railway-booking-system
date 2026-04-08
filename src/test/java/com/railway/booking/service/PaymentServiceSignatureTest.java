package com.railway.booking.service;

import com.railway.booking.dao.PaymentDAO;
import com.railway.booking.dao.TicketDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceSignatureTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentDAO paymentDAO = Mockito.mock(PaymentDAO.class);
        TicketDAO ticketDAO = Mockito.mock(TicketDAO.class);
        paymentService = new PaymentService(paymentDAO, ticketDAO);
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret_123");
    }

    @Test
    void verifyRazorpaySignature_returnsTrue_forValidSignature() throws Exception {
        String orderId = "order_123";
        String paymentId = "pay_123";
        String signature = hmacSha256Hex("test_secret_123", orderId + "|" + paymentId);

        boolean valid = paymentService.verifyRazorpaySignature(orderId, paymentId, signature);

        assertTrue(valid);
    }

    @Test
    void verifyRazorpaySignature_returnsFalse_forInvalidSignature() {
        boolean valid = paymentService.verifyRazorpaySignature("order_123", "pay_123", "invalid_signature");
        assertFalse(valid);
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

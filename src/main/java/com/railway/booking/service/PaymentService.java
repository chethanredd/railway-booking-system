package com.railway.booking.service;

import com.railway.booking.dao.PaymentDAO;
import com.railway.booking.dao.TicketDAO;
import com.railway.booking.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SERVICE: PaymentService — Gateway, refund (as in MVC diagram)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentDAO paymentDAO;
    private final TicketDAO  ticketDAO;

    public PaymentService(PaymentDAO paymentDAO, TicketDAO ticketDAO) {
        this.paymentDAO = paymentDAO;
        this.ticketDAO = ticketDAO;
    }

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public Map<String, Object> initiatePayment(String pnr, String method, double amount) {
        Ticket ticket = ticketDAO.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + pnr));

        Map<String, Object> response = new HashMap<>();

        if ("RAZORPAY".equalsIgnoreCase(method)) {
            try {
                com.razorpay.RazorpayClient client = new com.razorpay.RazorpayClient(razorpayKeyId, razorpayKeySecret);
                org.json.JSONObject orderRequest = new org.json.JSONObject();
                orderRequest.put("amount", (int)(amount * 100)); // amount in paise
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

                com.razorpay.Order order = client.orders.create(orderRequest);
                response.put("orderId", order.get("id"));
            } catch (com.razorpay.RazorpayException e) {
                log.error("Error creating Razorpay order", e);
                throw new RuntimeException("Failed to initiate Razorpay payment", e);
            }
        } else {
            response.put("orderId", "ORDER_" + System.currentTimeMillis());
        }

        response.put("amount", amount);
        response.put("currency", "INR");
        response.put("pnr", pnr);
        response.put("method", method);
        response.put("status", "INITIATED");

        log.info("Payment initiated: PNR={}, Amount=₹{}, Method={}", pnr, amount, method);
        return response;
    }

    public Payment getPaymentByTicket(Ticket ticket) {
        return paymentDAO.findByTicket(ticket).orElse(null);
    }

    public Payment getPaymentByTxnId(String txnId) {
        return paymentDAO.findByTransactionId(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    public double getTotalRevenue() {
        return paymentDAO.findByStatus(Payment.PaymentStatus.SUCCESS)
                .stream()
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    public long getTotalBookings() {
        return paymentDAO.countSuccessfulPayments();
    }

    public List<Payment> getAllPayments() {
        return paymentDAO.findAll();
    }

    public boolean verifyRazorpaySignature(String razorpayOrderId,
                                           String razorpayPaymentId,
                                           String razorpaySignature) {
        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return false;
        }
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            log.error("Razorpay signature verification failed: secret key is not configured");
            return false;
        }

        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    razorpaySignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Razorpay signature verification error", e);
            return false;
        }
    }
}

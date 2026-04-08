package com.railway.booking.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MODEL: Payment — gateway, refund (as in MVC diagram)
 */
@Document(collection = "payments")
public class Payment {

    @Id
    private String paymentId;

    private String transactionId;

    @DBRef
    private Ticket ticket;

    private double amount;

    private double refundAmount;

    private PaymentMethod method = PaymentMethod.UPI;

    private PaymentStatus status = PaymentStatus.PENDING;

    private String gatewayReference;

    private LocalDateTime paidAt = LocalDateTime.now();

    private LocalDateTime refundedAt;

    public void generateTxnId() {
        if (this.transactionId == null) {
            this.transactionId = "RWP" + System.currentTimeMillis();
        }
    }

    public enum PaymentMethod {
        UPI, NET_BANKING, CREDIT_CARD, DEBIT_CARD, WALLET, IRCTC_WALLET, EMI, RAZORPAY
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }

    // ── Constructors ──────────────────────────────────────────────
    public Payment() {
        generateTxnId();
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getGatewayReference() { return gatewayReference; }
    public void setGatewayReference(String gatewayReference) { this.gatewayReference = gatewayReference; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    // ── Builder ───────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String paymentId; private Ticket ticket; private double amount; private double refundAmount;
        private PaymentMethod method = PaymentMethod.UPI;
        private PaymentStatus status = PaymentStatus.PENDING;
        private String gatewayReference;
        public Builder paymentId(String v)       { this.paymentId=v; return this; }
        public Builder ticket(Ticket v)          { this.ticket=v; return this; }
        public Builder amount(double v)          { this.amount=v; return this; }
        public Builder refundAmount(double v)    { this.refundAmount=v; return this; }
        public Builder method(PaymentMethod v)   { this.method=v; return this; }
        public Builder status(PaymentStatus v)   { this.status=v; return this; }
        public Builder gatewayReference(String v){ this.gatewayReference=v; return this; }
        public Payment build() {
            Payment p = new Payment();
            p.paymentId=paymentId; p.ticket=ticket; p.amount=amount; p.refundAmount=refundAmount;
            p.method=method; p.status=status; p.gatewayReference=gatewayReference;
            return p;
        }
    }
}

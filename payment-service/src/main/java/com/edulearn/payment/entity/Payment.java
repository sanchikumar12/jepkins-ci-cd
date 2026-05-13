package com.edulearn.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Payment entity to track all course purchases
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;

    @Column(nullable = false)
    private Integer studentId;

    @Column(nullable = false)
    private Integer courseId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status; // "PENDING", "SUCCESS", "FAILED", "REFUNDED"

    @Column(nullable = false)
    private String mode; // "UPI", "CARD", "NET_BANKING", "WALLET"

    @Column(name = "order_id")
    private String orderId; 

    @Column(name = "transaction_id")
    private String transactionId; 

    @Column(name = "paid_at")
    // Fix applied here
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private String currency; 

    @Column(name = "created_at", nullable = false, updatable = false)
    // Fix applied here
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currency == null) {
            currency = "INR";
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
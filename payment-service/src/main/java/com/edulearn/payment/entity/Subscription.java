package com.edulearn.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Subscription entity for subscription plans
 * Allows students to subscribe for unlimited course access
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer subscriptionId;

    @Column(nullable = false)
    private Integer studentId;

    @Column(nullable = false)
    private String plan; // "FREE", "MONTHLY", "ANNUAL"

    @Column(name = "start_date", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Column(nullable = false)
    private String status; // "ACTIVE", "CANCELLED", "EXPIRED"

    @Column(name = "amount_paid")
    private Double amountPaid;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;

    @PrePersist
    protected void onCreate() {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (autoRenew == null) {
            autoRenew = false;
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
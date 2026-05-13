package com.edulearn.payment.entity;

import lombok.Data;

@Data
public class PaymentSuccessEvent {
    private String orderId;
    private String paymentId;
    private Integer userId;
    private Integer courseId;
    private Integer amount;

    // getters & setters
}
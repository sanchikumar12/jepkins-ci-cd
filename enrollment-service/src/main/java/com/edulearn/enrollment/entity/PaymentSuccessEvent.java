package com.edulearn.enrollment.entity;

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
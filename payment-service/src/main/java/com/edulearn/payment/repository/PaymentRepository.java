package com.edulearn.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edulearn.payment.entity.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity
 * Handles all database operations for payments
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByStudentId(Integer studentId);

    List<Payment> findByCourseId(Integer courseId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByTransactionId(String transactionId);
 //  
    List<Payment> findByCourseIdAndStatus(Integer courseId, String status);
     
      List<Payment> findByStudentIdOrderByCreatedAtDesc(Integer studentId);
      
      
    

   

   

    

   

    
}


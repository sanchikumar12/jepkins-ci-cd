package com.edulearn.payment.service;

import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.PaymentSuccessEvent;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.repository.PaymentRepository;
import com.edulearn.payment.repository.SubscriptionRepository;
import com.edulearn.notification.event.PaymentEvent;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of PaymentService
 * Handles payment processing with Razorpay integration
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private KafkaTemplate<String,PaymentSuccessEvent> kafkaTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${enrollment.service.url:http://localhost:8082/api/v1}")
    private String enrollmentServiceUrl;

    private RazorpayClient razorpayClient;


     //Initialize Razorpay client
    @Autowired
    public void initRazorpay() {
        try {
            this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Razorpay client: " + e.getMessage());
        }
    }
    
   
    /**
     * Create a payment order with Razorpay
     * Returns orderId to be used in frontend checkout
     */
    @Override
    public Map<String, String> createOrder(Integer studentId, Integer courseId, Double amount) {
        try {
            long amountInPaise = (long) (amount * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + studentId + "_" + courseId);

             com.razorpay.Order order = razorpayClient.orders.create(orderRequest);

             // Create Payment record with PENDING status
             Payment payment = new Payment();
             payment.setStudentId(studentId);
             payment.setCourseId(courseId);
             payment.setAmount(amount);
             payment.setStatus("PENDING");
             payment.setMode("ONLINE");
             payment.setCurrency("INR");
             payment.setOrderId(order.get("id"));
             

             paymentRepository.save(payment);

            Map<String, String> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("currency", "INR");
            response.put("amount", String.valueOf(amountInPaise));

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    /**
     * Verify Razorpay payment signature
     * Mark payment as SUCCESS and automatically enroll student in course
     */

    
    
    @Override
    @Transactional
    public Payment verifyPayment(String razorpayOrderId, String razorpayPaymentId,
                                 String razorpaySignature, Integer studentId, Integer courseId) {
        try {
            // --- Step 1-3: Signature Verification ---
            // (Uncomment and implement when ready for production security)
            /*
            String signatureData = razorpayOrderId + "|" + razorpayPaymentId;
            String computedSignature = generateSignature(signatureData, razorpayKeySecret);
            if (!computedSignature.equals(razorpaySignature)) {
                throw new RuntimeException("Payment verification failed: Signature mismatch");
            }
            */

            // --- Step 4: Find payment record ---
            // Fetch the most recent payment for the student
            Payment payment = paymentRepository
                    .findByStudentIdOrderByCreatedAtDesc(studentId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment record not found for student: " + studentId));

            // --- Step 5: Update payment status ---
            payment.setStatus("SUCCESS");
            payment.setTransactionId(razorpayPaymentId);
            payment.setPaidAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);
            
            // --- Step 6: Messaging / Integration ---
            try {
                // Prepare the event for Kafka
                PaymentSuccessEvent event = new PaymentSuccessEvent();
                event.setOrderId(payment.getOrderId());
                event.setPaymentId(Integer.toString(payment.getPaymentId()));
                event.setUserId(payment.getStudentId());
                event.setCourseId(payment.getCourseId());
                event.setAmount((int) Math.round(payment.getAmount()));

                // Send to Kafka topic
                kafkaTemplate.send("payment.success", event);
                
            } catch (Exception e) {
                // Log warning but do not roll back the payment status update
                System.err.println("Warning: Kafka event failed to send: " + e.getMessage());
            }

            // --- Step 7: Notification Event ---
            /* eventPublisher.publishEvent(
                new PaymentEvent(this, studentId, payment.getAmount(), "Course " + courseId)
            );
            */

            return savedPayment;

        } catch (Exception e) {
            // Final catch for any unhandled logic or database errors
            throw new RuntimeException("Payment verification error: " + e.getMessage());
        }
    }
  
        
       

    /**
     * Calls enrollment-service HTTP endpoint to enroll student
     * This works when enrollment-service is running on a separate port
     */
    private void enrollStudentViaHttp(Integer studentId, Integer courseId) {
        try {
            String url = enrollmentServiceUrl + "/enrollments/enroll";

            Map<String, Integer> enrollmentRequest = new HashMap<>();
            enrollmentRequest.put("studentId", studentId);
            enrollmentRequest.put("courseId", courseId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(enrollmentRequest, headers);

            restTemplate.postForObject(url, request, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to enroll student: " + e.getMessage());
        }
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateSignature(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public List<Payment> getPaymentsByStudent(Integer studentId) {
        return paymentRepository.findByStudentId(studentId);
    }

     

    @Override
    @Transactional
    public Subscription subscribe(Integer studentId, String plan) {
        // Check if student already has active subscription
        Optional<Subscription> existingSub = subscriptionRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE");

        if (existingSub.isPresent()) {
            throw new RuntimeException("Already have an active subscription");
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate;

        // Calculate end date based on plan
        switch (plan.toUpperCase()) {
            case "FREE":
                endDate = startDate.plusDays(36500); // Effectively never expires
                break;
            case "MONTHLY":
                endDate = startDate.plusDays(30);
                break;
            case "ANNUAL":
                endDate = startDate.plusDays(365);
                 break;
             default:
                 throw new RuntimeException("Invalid subscription plan: " + plan);
         }

         Subscription subscription = new Subscription();
         subscription.setStudentId(studentId);
         subscription.setPlan(plan);
         subscription.setStartDate(startDate);
         subscription.setEndDate(endDate);
         subscription.setStatus("ACTIVE");
         subscription.setAutoRenew(false);

         return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void cancelSubscription(Integer studentId) {
        Optional<Subscription> subOpt = subscriptionRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE");

        if (subOpt.isPresent()) {
            Subscription subscription = subOpt.get();
            subscription.setStatus("CANCELLED");
            subscriptionRepository.save(subscription);
        } else {
            throw new RuntimeException("No active subscription found");
        }
    }

    @Override
    public boolean isSubscriptionActive(Integer studentId) {
        Optional<Subscription> subOpt = subscriptionRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE");

        if (subOpt.isEmpty()) {
            return false;
        }

        Subscription subscription = subOpt.get();
        return subscription.getEndDate().isAfter(LocalDate.now());
    }

    @Override
    @Transactional
    public Payment refundPayment(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus("REFUNDED");
        return paymentRepository.save(payment);
    }

    @Override
    public Optional<Subscription> getSubscriptionByStudent(Integer studentId) {
        return subscriptionRepository.findByStudentIdAndStatus(studentId, "ACTIVE");
    }
    
    //Testing-->
    @Override
    @Transactional
    public String vef() {
        // Start a single try block for the whole function logic
        try {
            // Step 6: Call enrollment-service via HTTP to enroll student
            try {
                PaymentSuccessEvent event = new PaymentSuccessEvent();
                event.setOrderId("shhbmn1");
                event.setPaymentId("rth123");
                event.setUserId(5776);
                event.setCourseId(890988); 
                event.setAmount(1234);
                
                kafkaTemplate.send("payment.success", event);
            } catch (Exception e) {
                // Inner catch: Log error but keep going
                System.err.println("Warning: Could not enroll student via HTTP: " + e.getMessage());
            }

            return "Sussesfully Sent";

        } catch (Exception e) {
            // Outer catch: Handles any major failure in the function
            throw new RuntimeException("Payment verification error: " + e.getMessage());
        }
    }
    
    @Override
    public List<Payment> getPaymentHistory(Integer studentId) {
        return paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }


	@Override
	public List<Payment> getSuccessfulPaymentsByCourse(Integer courseId) {
		return paymentRepository.findByCourseIdAndStatus(courseId, "SUCCESS");
	}


	
}


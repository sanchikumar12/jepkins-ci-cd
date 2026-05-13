package com.edulearn.enrollment.service;

import com.edulearn.enrollment.entity.Enrollment;
import com.edulearn.enrollment.entity.PaymentSuccessEvent;
import com.edulearn.enrollment.repository.EnrollmentRepository;
import com.edulearn.notification.event.EnrollmentEvent;
import com.edulearn.notification.event.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;
    
    @Autowired
    private RestTemplate restTemplate;

    @Value("${course.service.base-url:${COURSE_SERVICE_BASE_URL:http://course-service-svc:80/api/v1/courses}}")
    private String courseServiceBaseUrl;

    @Value("${auth.service.base-url:${AUTH_SERVICE_BASE_URL:http://auth-service-svc:80/api/v1/auth/users}}")
    private String authServiceBaseUrl;

    @Override
    @Transactional
    public Enrollment enroll(Long studentId, Integer courseId) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new RuntimeException("Student already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .enrolledAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(0)
                .certificateIssued(false)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        eventPublisher.publishEvent(
                new EnrollmentEvent(this, Math.toIntExact(studentId), courseId, "Course " + courseId)
        );

        NotificationEvent notificationEvent = new NotificationEvent();
        notificationEvent.setUserId(Math.toIntExact(studentId));
        notificationEvent.setType("ENROLLMENT");
        notificationEvent.setTitle("Enrolled successfully!");
        notificationEvent.setMessage("You have enrolled in Course " + courseId + ". Start learning now!");
        notificationEvent.setRelatedEntityId(courseId);
        notificationEvent.setRelatedEntityType("COURSE");
        notificationEvent.setSourceService("enrollment-service");
        notificationKafkaTemplate.send("notification", notificationEvent);
        
        return saved;
    }

    @Override
    @Transactional
    public void unenroll(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with ID: " + enrollmentId));

        enrollment.setStatus("CANCELLED");
        enrollmentRepository.save(enrollment);
    }

    @Override
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Override
    public List<Enrollment> getEnrollmentsByCourse(Integer courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Override
    @Transactional
    public void updateProgress(Long studentId, Integer courseId, Integer progressPercent) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setProgressPercent(progressPercent);

        if (progressPercent == 100) {
            enrollment.setStatus("COMPLETED");
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void markComplete(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with ID: " + enrollmentId));

        enrollment.setStatus("COMPLETED");
        enrollment.setCompletedAt(LocalDateTime.now());
        enrollment.setProgressPercent(100);
        enrollmentRepository.save(enrollment);
    }

    @Override
    public boolean isEnrolled(Long studentId, Integer courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    public int getEnrollmentCount(Integer courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    @Transactional
    public byte[] generateCertificate(Long studentId, Integer courseId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalStateException("Enrollment not found for this student and course"));

        if (enrollment.getProgressPercent() == null || enrollment.getProgressPercent() < 100) {
            throw new IllegalStateException("Certificate is available only after 100% course completion");
        }

        if (enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollment.setStatus("COMPLETED");
        enrollment.setCertificateIssued(true);
        enrollmentRepository.save(enrollment);

        String studentName = fetchStudentName(studentId);
        String courseTitle = fetchCourseTitle(courseId);

        return createCertificateImage(studentName, courseTitle, enrollment.getCompletedAt());
    }

    //Consume ==>
    @KafkaListener(topics = "payment.success", groupId = "enrollment-group")
    public void consume(PaymentSuccessEvent event) {
        System.out.println("Received Payment Event: " + event);

        // 1. Idempotency check: Prevent duplicate enrollments
        boolean alreadyExists = enrollmentRepository
                .existsByStudentIdAndCourseId(Long.valueOf(event.getUserId()), event.getCourseId());

        if (alreadyExists) {
            System.out.println("User already enrolled in this course. Skipping...");
            return;
        }

        // 2. Map Event data to Enrollment Entity
        Enrollment enrollment = Enrollment.builder()
                .studentId(Long.valueOf(event.getUserId()))
                .courseId(event.getCourseId())
                .enrolledAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(0)
                .certificateIssued(false)
                .build();

        // 3. Save to Database
        enrollmentRepository.save(enrollment);
        System.out.println("Successfully enrolled user " + event.getUserId() + " in course " + event.getCourseId());

        String courseTitle = fetchCourseTitle(event.getCourseId());
        if (courseTitle == null || courseTitle.isBlank()) {
            courseTitle = "Course " + event.getCourseId();
        }

        NotificationEvent studentPaymentEvent = new NotificationEvent();
        studentPaymentEvent.setUserId(event.getUserId());
        studentPaymentEvent.setType("PAYMENT");
        studentPaymentEvent.setTitle("Payment done successfully");
        studentPaymentEvent.setMessage("You paid INR " + event.getAmount() + " for " + courseTitle + ".");
        studentPaymentEvent.setRelatedEntityId(event.getCourseId());
        studentPaymentEvent.setRelatedEntityType("COURSE");
        studentPaymentEvent.setSourceService("enrollment-service");
        notificationKafkaTemplate.send("notification", studentPaymentEvent);

        NotificationEvent studentEnrollmentEvent = new NotificationEvent();
        studentEnrollmentEvent.setUserId(event.getUserId());
        studentEnrollmentEvent.setType("ENROLLMENT");
        studentEnrollmentEvent.setTitle("Enrolled successfully!");
        studentEnrollmentEvent.setMessage("You have enrolled in " + courseTitle + ". Start learning now!");
        studentEnrollmentEvent.setRelatedEntityId(event.getCourseId());
        studentEnrollmentEvent.setRelatedEntityType("COURSE");
        studentEnrollmentEvent.setSourceService("enrollment-service");
        notificationKafkaTemplate.send("notification", studentEnrollmentEvent);

        Integer instructorId = fetchInstructorId(event.getCourseId());
        if (instructorId != null) {
            NotificationEvent instructorEnrollmentEvent = new NotificationEvent();
            instructorEnrollmentEvent.setUserId(instructorId);
            instructorEnrollmentEvent.setType("ENROLLMENT");
            instructorEnrollmentEvent.setTitle("New student enrolled");
            instructorEnrollmentEvent.setMessage("A new student has enrolled in your course " + courseTitle + ".");
            instructorEnrollmentEvent.setRelatedEntityId(event.getCourseId());
            instructorEnrollmentEvent.setRelatedEntityType("COURSE");
            instructorEnrollmentEvent.setSourceService("enrollment-service");
            notificationKafkaTemplate.send("notification", instructorEnrollmentEvent);

            NotificationEvent instructorPaymentEvent = new NotificationEvent();
            instructorPaymentEvent.setUserId(instructorId);
            instructorPaymentEvent.setType("PAYMENT");
            instructorPaymentEvent.setTitle("Payment received");
            instructorPaymentEvent.setMessage("Payment of INR " + event.getAmount() + " received for " + courseTitle + ".");
            instructorPaymentEvent.setRelatedEntityId(event.getCourseId());
            instructorPaymentEvent.setRelatedEntityType("COURSE");
            instructorPaymentEvent.setSourceService("enrollment-service");
            notificationKafkaTemplate.send("notification", instructorPaymentEvent);
        }
    }

    private Integer fetchInstructorId(Integer courseId) {
        try {
            String courseServiceUrl = courseServiceBaseUrl + "/" + courseId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(courseServiceUrl, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null || data.get("instructorId") == null) {
                return null;
            }
            return ((Number) data.get("instructorId")).intValue();
        } catch (Exception ex) {
            System.err.println("Warning: Could not fetch instructorId for course " + courseId + ": " + ex.getMessage());
            return null;
        }
    }

    private String fetchCourseTitle(Integer courseId) {
        try {
            String courseServiceUrl = courseServiceBaseUrl + "/" + courseId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(courseServiceUrl, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return "Course " + courseId;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null || data.get("title") == null) {
                return "Course " + courseId;
            }

            return String.valueOf(data.get("title"));
        } catch (Exception ex) {
            System.err.println("Warning: Could not fetch course title for course " + courseId + ": " + ex.getMessage());
            return "Course " + courseId;
        }
    }

    private String fetchStudentName(Long studentId) {
        try {
            String authServiceUrl = authServiceBaseUrl + "/" + studentId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(authServiceUrl, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return "Student " + studentId;
            }
            Object fullName = response.get("fullName");
            return fullName != null ? String.valueOf(fullName) : "Student " + studentId;
        } catch (Exception ex) {
            System.err.println("Warning: Could not fetch student name for user " + studentId + ": " + ex.getMessage());
            return "Student " + studentId;
        }
    }

    private byte[] createCertificateImage(String studentName, String courseTitle, LocalDateTime completedAt) {
        final int width = 1600;
        final int height = 1100;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            GradientPaint background = new GradientPaint(0, 0, new Color(245, 247, 255), width, height, new Color(236, 253, 245));
            g.setPaint(background);
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(30, 41, 59));
            g.setStroke(new BasicStroke(4f));
            g.draw(new RoundRectangle2D.Double(70, 70, width - 140, height - 140, 36, 36));

            g.setColor(new Color(15, 23, 42));
            g.setFont(new Font("Serif", Font.BOLD, 64));
            drawCenteredText(g, "Certificate of Completion", width, 220);

            g.setColor(new Color(71, 85, 105));
            g.setFont(new Font("SansSerif", Font.PLAIN, 30));
            drawCenteredText(g, "This certifies that", width, 320);

            g.setColor(new Color(79, 70, 229));
            g.setFont(new Font("Serif", Font.BOLD, 72));
            drawCenteredText(g, studentName, width, 430);

            g.setColor(new Color(51, 65, 85));
            g.setFont(new Font("SansSerif", Font.PLAIN, 32));
            drawCenteredText(g, "has successfully completed the course", width, 505);

            g.setColor(new Color(2, 132, 199));
            g.setFont(new Font("Serif", Font.BOLD, 52));
            drawCenteredText(g, courseTitle, width, 600);

            String completedOn = completedAt != null
                    ? completedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            g.setColor(new Color(71, 85, 105));
            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            drawCenteredText(g, "Completed on: " + completedOn, width, 700);

            g.setColor(new Color(16, 185, 129));
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            drawCenteredText(g, "EduLearn Verified Achievement", width, 820);

            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font("SansSerif", Font.PLAIN, 22));
            drawCenteredText(g, "Generated by EduLearn Application", width, 900);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to generate certificate image", ex);
        } finally {
            g.dispose();
        }
    }

    private void drawCenteredText(Graphics2D g, String text, int canvasWidth, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = (canvasWidth - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}



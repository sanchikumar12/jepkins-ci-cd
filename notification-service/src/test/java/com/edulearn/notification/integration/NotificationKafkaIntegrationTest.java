package com.edulearn.notification.integration;

import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.event.NotificationEvent;
import com.edulearn.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"notification"})
class NotificationKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Test
    void shouldConsumeEnrollmentAndPaymentNotificationEventsAndSaveToDatabase() throws Exception {
        notificationRepository.deleteAll();

        NotificationEvent enrollmentEvent = new NotificationEvent();
        enrollmentEvent.setUserId(101);
        enrollmentEvent.setType("ENROLLMENT");
        enrollmentEvent.setTitle("Enrolled successfully!");
        enrollmentEvent.setMessage("You have enrolled in Course 25.");
        enrollmentEvent.setRelatedEntityId(25);
        enrollmentEvent.setRelatedEntityType("COURSE");
        enrollmentEvent.setSourceService("enrollment-service");

        NotificationEvent paymentEvent = new NotificationEvent();
        paymentEvent.setUserId(101);
        paymentEvent.setType("PAYMENT");
        paymentEvent.setTitle("Payment successful");
        paymentEvent.setMessage("Payment completed for Course 25.");
        paymentEvent.setRelatedEntityId(25);
        paymentEvent.setRelatedEntityType("COURSE");
        paymentEvent.setSourceService("payment-service");

        kafkaTemplate.send("notification", enrollmentEvent).get();
        kafkaTemplate.send("notification", paymentEvent).get();

        List<Notification> saved = waitForNotifications(101, 2, 10000);
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(n -> "ENROLLMENT".equals(n.getType())));
        assertTrue(saved.stream().anyMatch(n -> "PAYMENT".equals(n.getType())));
    }

    private List<Notification> waitForNotifications(Integer userId, int expectedCount, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            List<Notification> notifications = notificationRepository.findByUserId(userId);
            if (notifications.size() >= expectedCount) {
                return notifications;
            }
            Thread.sleep(250);
        }
        return notificationRepository.findByUserId(userId);
    }
}

package com.edulearn.notification.integration;

import com.edulearn.notification.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"notification"})
class NotificationApiKafkaSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Test
    @WithMockUser(authorities = "STUDENT")
    void shouldConsumeKafkaAndExposeSavedNotificationsViaApi() throws Exception {
        NotificationEvent enrollmentEvent = new NotificationEvent();
        enrollmentEvent.setUserId(2201);
        enrollmentEvent.setType("ENROLLMENT");
        enrollmentEvent.setTitle("Enrolled successfully!");
        enrollmentEvent.setMessage("Enrollment is active for Course 77.");
        enrollmentEvent.setRelatedEntityId(77);
        enrollmentEvent.setRelatedEntityType("COURSE");
        enrollmentEvent.setSourceService("enrollment-service");

        NotificationEvent paymentEvent = new NotificationEvent();
        paymentEvent.setUserId(2201);
        paymentEvent.setType("PAYMENT");
        paymentEvent.setTitle("Payment successful");
        paymentEvent.setMessage("Payment done for Course 77.");
        paymentEvent.setRelatedEntityId(77);
        paymentEvent.setRelatedEntityType("COURSE");
        paymentEvent.setSourceService("payment-service");

        kafkaTemplate.send("notification", enrollmentEvent).get();
        kafkaTemplate.send("notification", paymentEvent).get();

        waitForApiCount(2201, 2, 10000);

        mockMvc.perform(get("/api/notifications/user/2201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/notifications/type").param("userId", "2201").param("type", "ENROLLMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/notifications/type").param("userId", "2201").param("type", "PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private void waitForApiCount(int userId, int expectedCount, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            MvcResult result = mockMvc.perform(get("/api/notifications/user/" + userId))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            int count = countTopLevelJsonArray(body);
            if (count >= expectedCount) {
                return;
            }
            Thread.sleep(250);
        }
    }

    private int countTopLevelJsonArray(String json) {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '[' || trimmed.charAt(trimmed.length() - 1) != ']') {
            return 0;
        }
        if ("[]".equals(trimmed)) {
            return 0;
        }
        int depth = 0;
        int count = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '{') {
                if (depth == 0) {
                    count++;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
            }
        }
        return count;
    }
}

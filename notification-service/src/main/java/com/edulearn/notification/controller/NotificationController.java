package com.edulearn.notification.controller;

import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getNotificationsByUser(@PathVariable Integer userId) {
        return notificationService.getNotificationsByUser(userId);
    }

    @GetMapping("/unread/{userId}")
    public List<Notification> getUnreadNotifications(@PathVariable Integer userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/unread-count/{userId}")
    public int getUnreadCount(@PathVariable Integer userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PutMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable Integer notificationId) {
        notificationService.markAsRead(notificationId);
    }

    @PutMapping("/read-all/{userId}")
    public void markAllAsRead(@PathVariable Integer userId) {
        notificationService.markAllAsRead(userId);
    }

    @DeleteMapping("/{notificationId}")
    public void deleteNotification(@PathVariable Integer notificationId) {
        notificationService.deleteNotification(notificationId);
    }

    @GetMapping("/type")
    public List<Notification> getNotificationsByType(
            @RequestParam Integer userId,
            @RequestParam String type) {
        return notificationService.getNotificationsByType(userId, type);
    }
}

package com.contractcompiler.notification;

/**
 * Interface for sending notifications (email).
 * Implementations: {@link SendGridNotificationService} for production,
 * mock/log implementation for testing.
 */
public interface NotificationService {

    /**
     * Sends a counsel question package email.
     *
     * @param toEmail    recipient email address
     * @param toName     recipient name (nullable)
     * @param subject    email subject
     * @param htmlBody   HTML email body
     */
    void sendCounselEmail(String toEmail, String toName, String subject, String htmlBody);
}

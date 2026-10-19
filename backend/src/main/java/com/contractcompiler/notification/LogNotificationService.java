package com.contractcompiler.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Logs emails instead of sending them. Active when SendGrid API key is not configured.
 */
@Service
@ConditionalOnProperty(name = "app.notification.sendgrid-api-key", havingValue = "", matchIfMissing = true)
public class LogNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void sendCounselEmail(String toEmail, String toName, String subject, String htmlBody) {
        log.info("EMAIL [to={}({}), subject={}] — body logged at DEBUG level", toEmail, toName, subject);
        log.debug("EMAIL body:\n{}", htmlBody);
    }
}

package com.ajo.platform.modules.notifications.service;

import com.ajo.platform.modules.notifications.config.SendGridConfig;
import com.ajo.platform.modules.notifications.config.TermiiConfig;
import com.ajo.platform.modules.notifications.dto.NotificationRequest;
import com.ajo.platform.modules.notifications.dto.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TermiiConfig termiiConfig;
    private final SendGridConfig sendGridConfig;
    private final ObjectMapper objectMapper;

    public NotificationResponse sendNotification(NotificationRequest request) {

        boolean smsSent = false;
        boolean emailSent = false;

        try {
            if (request.getType() == NotificationRequest.NotificationType.SMS ||
                    request.getType() == NotificationRequest.NotificationType.BOTH) {
                smsSent = sendSMS(request.getRecipient(), request.getMessage());
            }

            if (request.getType() == NotificationRequest.NotificationType.EMAIL ||
                    request.getType() == NotificationRequest.NotificationType.BOTH) {
                emailSent = sendEmail(request.getRecipient(), request.getSubject(), request.getMessage());
            }

            return NotificationResponse.builder()
                    .smsSent(smsSent)
                    .emailSent(emailSent)
                    .message("Notification sent successfully")
                    .build();

        } catch (Exception e) {
            log.error("Notification failed: {}", e.getMessage());
            return NotificationResponse.builder()
                    .smsSent(false)
                    .emailSent(false)
                    .message("Notification failed: " + e.getMessage())
                    .build();
        }
    }

    private boolean sendSMS(String phoneNumber, String message) {
        try {
            Map<String, Object> payload = Map.of(
                    "to", phoneNumber,
                    "from", termiiConfig.getSenderId(),
                    "sms", message,
                    "type", "plain",
                    "channel", "generic",
                    "api_key", termiiConfig.getApiKey()
            );

            String payloadJson = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(termiiConfig.getBaseUrl() + "/api/sms/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("SMS sent to {}: {}", phoneNumber, response.body());
            return response.statusCode() == 200;

        } catch (Exception e) {
            log.error("SMS sending failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendEmail(String emailAddress, String subject, String message) {
        try {
            Map<String, Object> from = Map.of(
                    "email", sendGridConfig.getFromEmail(),
                    "name", sendGridConfig.getFromName()
            );

            Map<String, Object> to = Map.of(
                    "email", emailAddress
            );

            Map<String, Object> content = Map.of(
                    "type", "text/plain",
                    "value", message
            );

            Map<String, Object> personalization = Map.of(
                    "to", java.util.List.of(to),
                    "subject", subject
            );

            Map<String, Object> payload = Map.of(
                    "personalizations", java.util.List.of(personalization),
                    "from", from,
                    "content", java.util.List.of(content)
            );

            String payloadJson = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .header("Authorization", "Bearer " + sendGridConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("Email sent to {}: Status {}", emailAddress, response.statusCode());
            return response.statusCode() == 202;

        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage());
            return false;
        }
    }

    public void sendContributionReminder(String phoneNumber, String email,
                                         String groupName, Integer cycleNumber) {
        String message = String.format(
                "Reminder: Your contribution for %s - Cycle %d is due. Please make payment to avoid penalties.",
                groupName, cycleNumber
        );

        NotificationRequest request = NotificationRequest.builder()
                .recipient(phoneNumber)
                .subject("Contribution Reminder - " + groupName)
                .message(message)
                .type(NotificationRequest.NotificationType.BOTH)
                .build();

        sendNotification(request);
    }

    public void sendPayoutNotification(String phoneNumber, String email,
                                       String groupName, String amount) {
        String message = String.format(
                "Good news! Your payout of %s from %s has been processed. Check your account.",
                amount, groupName
        );

        NotificationRequest request = NotificationRequest.builder()
                .recipient(phoneNumber)
                .subject("Payout Received - " + groupName)
                .message(message)
                .type(NotificationRequest.NotificationType.BOTH)
                .build();

        sendNotification(request);
    }

    public void sendEmailNotification(String emailAddress, String subject, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .recipient(emailAddress)
                .subject(subject)
                .message(message)
                .type(NotificationRequest.NotificationType.EMAIL)
                .build();
        sendNotification(request);
    }

    public void sendSmsNotification(String phoneNumber, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .recipient(phoneNumber)
                .subject("Ajo Platform")
                .message(message)
                .type(NotificationRequest.NotificationType.SMS)
                .build();
        sendNotification(request);
    }

}
package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${BREVO_SENDER_EMAIL:}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Core method: sends via Brevo's HTTPS REST API (Port 443)
    public void sendEmail(String to, String subject, String body) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.err.println("BREVO_API_KEY is missing in Railway variables!");
            return;
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            System.err.println("BREVO_SENDER_EMAIL is missing in Railway variables!");
            return;
        }

        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        // Sender details (must match your Brevo verified login email)
        Map<String, String> sender = new HashMap<>();
        sender.put("name", "TaskFlow");
        sender.put("email", senderEmail);

        // Recipient details (any user's email)
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", to);

        // Construct request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", Collections.singletonList(recipient));
        payload.put("subject", subject);
        payload.put("textContent", body);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Email sent successfully via Brevo API: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Failed to send email via Brevo API: " + e.getMessage());
        }
    }

    // Called by TaskAutomationScheduler for upcoming deadlines
    public void sendReminder(String to, String taskTitle, LocalDateTime deadline) {
        String formattedDate = (deadline != null) ? deadline.format(FORMATTER) : "soon";
        String subject = "Upcoming Deadline Reminder: " + taskTitle;
        String body = "Hello,\n\nThis is a reminder that your task \"" + taskTitle 
                    + "\" has an upcoming deadline on " + formattedDate + ".\n\nBest regards,\nTaskFlow Team";
        sendEmail(to, subject, body);
    }

    // Called by TaskAutomationScheduler for overdue alerts
    public void sendOverdueAlert(String to, String taskTitle, LocalDateTime deadline) {
        String formattedDate = (deadline != null) ? deadline.format(FORMATTER) : "earlier";
        String subject = "Task Overdue Alert: " + taskTitle;
        String body = "Hello,\n\nYour task \"" + taskTitle 
                    + "\" was due on " + formattedDate + " and has passed its deadline.\n\nBest regards,\nTaskFlow Team";
        sendEmail(to, subject, body);
    }

    // Called by TaskAutomationScheduler for daily summaries
    public void sendMorningBriefing(String to, String userName, List<?> tasks) {
        StringBuilder body = new StringBuilder();
        body.append("Good morning, ").append(userName).append("!\n\n");
        body.append("Here is your daily task summary:\n");

        if (tasks == null || tasks.isEmpty()) {
            body.append("You have no pending tasks scheduled for today.\n");
        } else {
            for (Object task : tasks) {
                body.append("• ").append(task.toString()).append("\n");
            }
        }

        body.append("\nBest regards,\nTaskFlow Team");
        sendEmail(to, "Your Daily Morning Briefing - TaskFlow", body.toString());
    }
}
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

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Core method that calls Resend's HTTPS API
    public void sendEmail(String to, String subject, String body) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.err.println("RESEND_API_KEY is missing in Railway variables!");
            return;
        }

        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", "TaskFlow <onboarding@resend.dev>");
        payload.put("to", Collections.singletonList(to));
        payload.put("subject", subject);
        payload.put("text", body);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Email sent successfully via Resend API: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Failed to send email via Resend API: " + e.getMessage());
        }
    }

    // Called by TaskAutomationScheduler line 48 for upcoming deadlines
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
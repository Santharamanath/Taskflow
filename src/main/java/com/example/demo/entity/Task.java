package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String userEmail;
    private LocalDateTime deadline;
    private boolean completed = false;
    private boolean reminderSent = false;
    private boolean overdueSent = false; // Tracks if "deadline passed" email was sent
    private String priority = "MEDIUM";
    private String category = "General";

    public Task() {}

    public Task(String title, String userEmail, LocalDateTime deadline, String priority, String category) {
        this.title = title;
        this.userEmail = userEmail;
        this.deadline = deadline;
        this.priority = priority;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public boolean isOverdueSent() { return overdueSent; }
    public void setOverdueSent(boolean overdueSent) { this.overdueSent = overdueSent; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
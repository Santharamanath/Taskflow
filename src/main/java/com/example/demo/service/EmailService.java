package com.example.demo.service;

import com.example.demo.entity.Task;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 1. Upcoming Deadline Reminder (Contains Task Name prominently)
    public boolean sendReminder(String toEmail, String taskTitle, LocalDateTime deadline) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String formattedDate = deadline.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"));

            String htmlMsg = """
                <div style="font-family: Arial, sans-serif; max-width: 520px; margin: auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                    <div style="display: inline-block; background-color: #fef3c7; color: #b45309; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 12px;">
                        UPCOMING DEADLINE
                    </div>
                    <h2 style="color: #0f172a; margin: 0 0 10px; font-size: 22px;">Task Reminder: %s</h2>
                    <p style="font-size: 15px; color: #334155;">Your target is due soon:</p>
                    <div style="background-color: #f8fafc; border-left: 4px solid #f59e0b; padding: 14px 18px; border-radius: 8px; margin: 18px 0;">
                        <p style="margin: 0; color: #64748b; font-size: 12px; text-transform: uppercase; font-weight: bold;">Task Name</p>
                        <h3 style="margin: 4px 0 8px; color: #0f172a; font-size: 20px;">%s</h3>
                        <p style="margin: 0; color: #64748b; font-size: 14px;"><strong>Target Date:</strong> %s</p>
                    </div>
                    <p style="font-size: 13px; color: #64748b;">Visit your TaskFlow dashboard to mark it completed.</p>
                </div>
            """.formatted(taskTitle, taskTitle, formattedDate);

            helper.setTo(toEmail);
            helper.setSubject("⏳ Reminder: [" + taskTitle + "] is due soon");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            System.out.println(">>> [SUCCESS] Upcoming reminder for '" + taskTitle + "' sent to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println(">>> [ERROR] Reminder failed: " + e.getMessage());
            return false;
        }
    }

    // 2. Overdue / Missed Deadline Alert (Contains Task Name prominently)
    public boolean sendOverdueAlert(String toEmail, String taskTitle, LocalDateTime deadline) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String formattedDate = deadline.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"));

            String htmlMsg = """
                <div style="font-family: Arial, sans-serif; max-width: 520px; margin: auto; padding: 24px; border: 1px solid #fecaca; border-radius: 16px; background-color: #ffffff;">
                    <div style="display: inline-block; background-color: #fee2e2; color: #dc2626; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 12px;">
                        DEADLINE EXPIRED
                    </div>
                    <h2 style="color: #dc2626; margin: 0 0 10px; font-size: 22px;">Overdue: %s</h2>
                    <p style="font-size: 15px; color: #334155;">The deadline for your scheduled task has passed:</p>
                    <div style="background-color: #fff1f2; border-left: 4px solid #ef4444; padding: 14px 18px; border-radius: 8px; margin: 18px 0;">
                        <p style="margin: 0; color: #991b1b; font-size: 12px; text-transform: uppercase; font-weight: bold;">Task Name</p>
                        <h3 style="margin: 4px 0 8px; color: #991b1b; font-size: 20px;">%s</h3>
                        <p style="margin: 0; color: #b91c1c; font-size: 14px;"><strong>Was due:</strong> %s</p>
                    </div>
                    <p style="font-size: 13px; color: #64748b;">If you've finished this, log in to check it off your list.</p>
                </div>
            """.formatted(taskTitle, taskTitle, formattedDate);

            helper.setTo(toEmail);
            helper.setSubject("🚨 Deadline Passed: [" + taskTitle + "]");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            System.out.println(">>> [SUCCESS] Overdue alert for '" + taskTitle + "' sent to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println(">>> [ERROR] Overdue alert failed: " + e.getMessage());
            return false;
        }
    }

    // 3. Morning Briefing (Lists each Task Name)
    public boolean sendMorningBriefing(String toEmail, String userName, List<Task> todayTasks) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy"));
            StringBuilder taskRows = new StringBuilder();

            if (todayTasks.isEmpty()) {
                taskRows.append("""
                    <tr>
                        <td colspan="3" style="padding: 16px; text-align: center; color: #64748b; font-size: 14px;">
                            No tasks scheduled for today.
                        </td>
                    </tr>
                """);
            } else {
                for (Task task : todayTasks) {
                    String timeStr = task.getDeadline().format(DateTimeFormatter.ofPattern("hh:mm a"));
                    String badgeColor = switch (task.getPriority() != null ? task.getPriority() : "MEDIUM") {
                        case "HIGH" -> "#ef4444";
                        case "LOW" -> "#3b82f6";
                        default -> "#f59e0b";
                    };

                    taskRows.append("""
                        <tr style="border-bottom: 1px solid #f1f5f9;">
                            <td style="padding: 12px 8px; font-size: 14px; color: #0f172a; font-weight: 600;">%s</td>
                            <td style="padding: 12px 8px; font-size: 13px; color: #64748b;">%s</td>
                            <td style="padding: 12px 8px; text-align: right;">
                                <span style="background-color: %s; color: #ffffff; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: bold;">
                                    %s
                                </span>
                            </td>
                        </tr>
                    """.formatted(task.getTitle(), timeStr, badgeColor, task.getPriority()));
                }
            }

            String htmlMsg = """
                <div style="font-family: Arial, sans-serif; max-width: 560px; margin: auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                    <h2>Good morning, %s! ☀️</h2>
                    <p style="color: #64748b; font-size: 14px;">Here is your scheduled task list for %s:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 14px;">
                        <thead>
                            <tr style="border-bottom: 2px solid #e2e8f0; text-align: left;">
                                <th style="padding: 8px; font-size: 12px; color: #64748b; text-transform: uppercase;">Task Name</th>
                                <th style="padding: 8px; font-size: 12px; color: #64748b; text-transform: uppercase;">Time</th>
                                <th style="padding: 8px; font-size: 12px; color: #64748b; text-transform: uppercase; text-align: right;">Priority</th>
                            </tr>
                        </thead>
                        <tbody>%s</tbody>
                    </table>
                </div>
            """.formatted(userName, todayStr, taskRows.toString());

            helper.setTo(toEmail);
            helper.setSubject("☀️ Daily Briefing (" + todayTasks.size() + " tasks) — " + todayStr);
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            System.out.println(">>> [SUCCESS] Morning Briefing sent to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println(">>> [ERROR] Morning Briefing failed: " + e.getMessage());
            return false;
        }
    }
}
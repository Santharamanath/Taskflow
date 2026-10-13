package com.example.demo.scheduler;

import com.example.demo.entity.Task;
import com.example.demo.entity.User;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class TaskAutomationScheduler {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public TaskAutomationScheduler(TaskRepository taskRepository, UserRepository userRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Runs every 10 seconds to detect passed deadlines and upcoming tasks
    @Scheduled(fixedDelay = 10000)
    public void processDeadlineAlerts() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Check for tasks whose deadline has PASSED (deadline <= now)
        List<Task> overdueTasks = taskRepository.findOverdueTasks(now);
        for (Task task : overdueTasks) {
            System.out.println(">>> [TRIGGER] Deadline has passed for task: " + task.getTitle() + ". Sending overdue alert to " + task.getUserEmail());
            emailService.sendOverdueAlert(task.getUserEmail(), task.getTitle(), task.getDeadline());
            task.setOverdueSent(true);
            taskRepository.save(task);
        }

        // 2. Check for upcoming tasks (within next 2 hours)
        LocalDateTime upcomingWindow = now.plusHours(2);
        List<Task> upcomingTasks = taskRepository.findUpcomingTasks(now, upcomingWindow);
        for (Task task : upcomingTasks) {
            System.out.println(">>> [TRIGGER] Upcoming deadline for task: " + task.getTitle() + ". Sending reminder to " + task.getUserEmail());
            emailService.sendReminder(task.getUserEmail(), task.getTitle(), task.getDeadline());
            task.setReminderSent(true);
            taskRepository.save(task);
        }
    }

    // 7:00 AM IST Daily Morning Briefing
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void sendDailyMorningBriefings() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<Task> todayTasks = taskRepository.findTodayTasksForUser(user.getEmail(), startOfDay, endOfDay);
            emailService.sendMorningBriefing(user.getEmail(), user.getName(), todayTasks);
        }
    }

    // Daily midnight purge for tasks older than 6 months
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void purgeOldTasks() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        int deleted = taskRepository.deleteOlderThan(sixMonthsAgo);
        if (deleted > 0) {
            System.out.println("Purged " + deleted + " tasks older than 6 months.");
        }
    }
}
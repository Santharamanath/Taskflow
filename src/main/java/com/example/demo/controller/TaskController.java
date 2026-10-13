package com.example.demo.controller;

import com.example.demo.entity.Task;
import com.example.demo.entity.User;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public TaskController(TaskRepository taskRepository, UserRepository userRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // --- Authentication Routes ---

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("loggedUser") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/auth/register")
    public String registerUser(@RequestParam("name") String name,
                               @RequestParam("email") String email,
                               @RequestParam("password") String password,
                               HttpSession session) {
        if (userRepository.findByEmail(email).isPresent()) {
            return "redirect:/login?error=Email+already+registered";
        }
        User newUser = new User(name.trim(), email.trim().toLowerCase(), password);
        userRepository.save(newUser);
        session.setAttribute("loggedUser", newUser);
        return "redirect:/";
    }

    @PostMapping("/auth/login")
    public String loginUser(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            HttpSession session) {
        Optional<User> userOpt = userRepository.findByEmailAndPassword(email.trim().toLowerCase(), password);
        if (userOpt.isPresent()) {
            session.setAttribute("loggedUser", userOpt.get());
            return "redirect:/";
        }
        return "redirect:/login?error=Invalid+credentials";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- Main Dashboard ---

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        return "index";
    }

    // Diagnostic route to test general email alerts
    @GetMapping("/test-mail")
    @ResponseBody
    public String testMail(@RequestParam("to") String to) {
        try {
            emailService.sendReminder(to, "Instant System Test Task", LocalDateTime.now().plusHours(1));
            return "Email sent successfully to " + to + "! Check your Inbox and Spam folders.";
        } catch (Exception e) {
            return "Email Error: " + e.getMessage();
        }
    }

    // --- Instant Test Route for Idea 5: Daily Morning Briefing ---
    @GetMapping("/briefing-test")
    @ResponseBody
    public String testMorningBriefing(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) {
            return "Please log into the app first at http://localhost:8080/login, then visit this URL!";
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Task> todayTasks = taskRepository.findTodayTasksForUser(user.getEmail(), startOfDay, endOfDay);
        emailService.sendMorningBriefing(user.getEmail(), user.getName(), todayTasks);

        return "Morning Briefing email sent to " + user.getEmail() + " (" + todayTasks.size() + " tasks scheduled for today). Check your inbox!";
    }

    @GetMapping("/api/tasks")
    @ResponseBody
    public List<Task> getAllTasks(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return Collections.emptyList();
        return taskRepository.findByUserEmail(user.getEmail());
    }

    @PostMapping("/tasks")
    public String createTask(
            @RequestParam("title") String title,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") String priority,
            @RequestParam(value = "category", defaultValue = "General") String category,
            @RequestParam("deadline") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        taskRepository.save(new Task(title, user.getEmail(), deadline, priority, category));
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/toggle")
    @ResponseBody
    public Map<String, Object> toggleComplete(@PathVariable("id") Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return Map.of("success", false);

        Task task = taskRepository.findById(id).orElseThrow();
        if (!task.getUserEmail().equalsIgnoreCase(user.getEmail())) {
            return Map.of("success", false, "error", "Unauthorized");
        }

        task.setCompleted(!task.isCompleted());
        taskRepository.save(task);
        return Map.of("success", true, "completed", task.isCompleted());
    }

    @PostMapping("/tasks/{id}/reschedule")
    @ResponseBody
    public Map<String, Object> rescheduleTask(
            @PathVariable("id") Long id,
            @RequestParam("newDeadline") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDeadline,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return Map.of("success", false);

        Task task = taskRepository.findById(id).orElseThrow();
        if (!task.getUserEmail().equalsIgnoreCase(user.getEmail())) {
            return Map.of("success", false, "error", "Unauthorized");
        }

        task.setDeadline(newDeadline);
        task.setReminderSent(false);
        taskRepository.save(task);
        return Map.of("success", true);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseBody
    public Map<String, Object> deleteTask(@PathVariable("id") Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return Map.of("success", false);

        Task task = taskRepository.findById(id).orElseThrow();
        if (!task.getUserEmail().equalsIgnoreCase(user.getEmail())) {
            return Map.of("success", false, "error", "Unauthorized");
        }

        taskRepository.deleteById(id);
        return Map.of("success", true);
    }

    @GetMapping("/api/events")
    @ResponseBody
    public List<Map<String, Object>> getCalendarEvents(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return Collections.emptyList();

        List<Task> tasks = taskRepository.findByUserEmailAndDeadlineBetween(user.getEmail(), start, end);
        List<Map<String, Object>> events = new ArrayList<>();

        for (Task task : tasks) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", task.getId());
            event.put("title", (task.isCompleted() ? "✔ " : "") + "[" + task.getCategory() + "] " + task.getTitle());
            event.put("start", task.getDeadline().toString());

            String color;
            if (task.isCompleted()) {
                color = "#10b981";
            } else {
                color = switch (task.getPriority() != null ? task.getPriority() : "MEDIUM") {
                    case "HIGH" -> "#ef4444";
                    case "LOW" -> "#3b82f6";
                    default -> "#f59e0b";
                };
            }
            event.put("backgroundColor", color);
            event.put("borderColor", color);

            Map<String, Object> props = new HashMap<>();
            props.put("priority", task.getPriority());
            props.put("category", task.getCategory());
            props.put("email", task.getUserEmail());
            props.put("completed", task.isCompleted());
            event.put("extendedProps", props);

            events.add(event);
        }
        return events;
    }
}
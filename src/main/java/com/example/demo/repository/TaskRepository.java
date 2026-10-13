package com.example.demo.repository;

import com.example.demo.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserEmail(String userEmail);

    List<Task> findByUserEmailAndDeadlineBetween(String userEmail, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Task t WHERE t.userEmail = :email AND t.completed = false AND t.deadline BETWEEN :start AND :end ORDER BY t.deadline ASC")
    List<Task> findTodayTasksForUser(@Param("email") String email, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Tasks due within the upcoming window that haven't received advance warning
    @Query("SELECT t FROM Task t WHERE t.completed = false AND t.reminderSent = false AND t.deadline BETWEEN :now AND :window")
    List<Task> findUpcomingTasks(@Param("now") LocalDateTime now, @Param("window") LocalDateTime window);

    // Tasks whose deadline has already PASSED and haven't received the overdue alert
    @Query("SELECT t FROM Task t WHERE t.completed = false AND t.overdueSent = false AND t.deadline <= :now")
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM Task t WHERE t.deadline < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
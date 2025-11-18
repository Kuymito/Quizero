package com.example.quizapp.repository;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.QuizAttempt;
import com.example.quizapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /**
     * Finds all QuizAttempt records for a specific User (Student).
     */
    List<QuizAttempt> findByStudent(User student);

    /**
     * NEW: Finds all QuizAttempt records for a specific Quiz.
     * This will be used by the Teacher to see results.
     */
    List<QuizAttempt> findByQuiz(Quiz quiz);
}
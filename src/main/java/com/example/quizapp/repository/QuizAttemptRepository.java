package com.example.quizapp.repository;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.QuizAttempt;
import com.example.quizapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable; // Import Pageable

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudent(User student);

    List<QuizAttempt> findByQuiz(Quiz quiz);

    @Query("SELECT DISTINCT a FROM QuizAttempt a LEFT JOIN FETCH a.quiz WHERE a.student = :student")
    List<QuizAttempt> findByStudentAndFetchQuiz(@Param("student") User student);

    @Query("SELECT a FROM QuizAttempt a " +
            "LEFT JOIN FETCH a.quiz q " +
            "LEFT JOIN FETCH q.questions qn " +
            "LEFT JOIN FETCH qn.options " +
            "LEFT JOIN FETCH a.studentAnswers sa " +
            "LEFT JOIN FETCH sa.selectedOption " +
            "WHERE a.id = :id")
    Optional<QuizAttempt> findByIdAndFetchAllDetails(@Param("id") Long id);

    /**
     * NEW: Fetch top scorers for a quiz.
     * Orders by Score (Highest first), then by Time (Earliest/Fastest first).
     * Joins with Student to avoid LazyInitializationException.
     */
    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.student WHERE a.quiz = :quiz ORDER BY a.score DESC, a.attemptedAt ASC")
    List<QuizAttempt> findTopAttempts(@Param("quiz") Quiz quiz, Pageable pageable);

    Optional<QuizAttempt> findByStudentAndQuiz(User student, Quiz quiz);
}
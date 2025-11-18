package com.example.quizapp.repository;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.QuizAttempt;
import com.example.quizapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudent(User student);

    List<QuizAttempt> findByQuiz(Quiz quiz);

    /**
     * NEW: Finds attempts by student and EAGERLY loads the quiz
     * (for student/performance).
     */
    @Query("SELECT DISTINCT a FROM QuizAttempt a LEFT JOIN FETCH a.quiz WHERE a.student = :student")
    List<QuizAttempt> findByStudentAndFetchQuiz(@Param("student") User student);

    /**
     * NEW: Finds a single attempt by ID and EAGERLY loads ALL details
     * (for student/attempt-details).
     */
    @Query("SELECT a FROM QuizAttempt a " +
            "LEFT JOIN FETCH a.quiz q " +
            "LEFT JOIN FETCH q.questions qn " +
            "LEFT JOIN FETCH qn.options " +
            "LEFT JOIN FETCH a.studentAnswers sa " +
            "LEFT JOIN FETCH sa.selectedOption " +
            "WHERE a.id = :id")
    Optional<QuizAttempt> findByIdAndFetchAllDetails(@Param("id") Long id);
}
package com.example.quizapp.repository;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.QuizAttempt;
import com.example.quizapp.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudent(User student);

    List<QuizAttempt> findByQuiz(Quiz quiz);

    Optional<QuizAttempt> findByStudentAndQuiz(User student, Quiz quiz);

    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.student WHERE a.quiz.id = :quizId")
    List<QuizAttempt> findByQuizIdAndFetchStudent(@Param("quizId") Long quizId);

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
     * FIX: Leaderboard now only counts the FIRST attempt.
     * Logic: Select attempt 'a' ONLY IF its timestamp is the earliest (MIN) for that student and quiz.
     */
    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.student " +
            "WHERE a.quiz.id = :quizId " +
            "AND a.attemptedAt = (SELECT MIN(sub.attemptedAt) FROM QuizAttempt sub WHERE sub.student = a.student AND sub.quiz.id = :quizId) " +
            "ORDER BY a.score DESC, a.attemptedAt ASC")
    List<QuizAttempt> findTopAttempts(@Param("quizId") Long quizId, Pageable pageable);

    List<QuizAttempt> findTop5ByOrderByAttemptedAtDesc();

    @Query("SELECT qa FROM QuizAttempt qa " +
            "LEFT JOIN FETCH qa.student " +
            "LEFT JOIN FETCH qa.quiz " +
            "ORDER BY qa.attemptedAt DESC")
    List<QuizAttempt> findRecentAttempts(Pageable pageable);
}
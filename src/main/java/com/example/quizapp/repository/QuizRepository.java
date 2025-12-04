package com.example.quizapp.repository;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByTeacher(User teacher);

    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.teacher = :teacher")
    List<Quiz> findByTeacherAndFetchQuestions(@Param("teacher") User teacher);

    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions LEFT JOIN FETCH q.teacher")
    List<Quiz> findAllAndFetchQuestionsAndTeacher();

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions qn LEFT JOIN FETCH qn.options WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestionsAndOptions(@Param("id") Long id);

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestions(@Param("id") Long id);

    // NEW: Search quizzes by Title or Subject (case-insensitive)
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions LEFT JOIN FETCH q.teacher " +
            "WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.subject) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Quiz> searchQuizzes(@Param("search") String search);
}
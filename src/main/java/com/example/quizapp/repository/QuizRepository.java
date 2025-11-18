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

    // Original query: Find all quizzes created by a specific teacher
    List<Quiz> findByTeacher(User teacher);

    // --- ADD THE FOLLOWING METHODS ---

    /**
     * NEW: Finds quizzes by teacher and EAGERLY loads the questions
     * to prevent LazyInitializationException in the template.
     */
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.teacher = :teacher")
    List<Quiz> findByTeacherAndFetchQuestions(@Param("teacher") User teacher);

    /**
     * NEW: Finds all quizzes and EAGERLY loads the questions
     * to prevent LazyInitializationException in the student quiz list.
     * Use DISTINCT to avoid duplicates from the join.
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions")
    List<Quiz> findAllAndFetchQuestions();

    /**
     * NEW: Finds a single quiz by ID and EAGERLY loads the questions
     * to prevent LazyInitializationException in the "take-quiz" page.
     */
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestions(@Param("id") Long id);
}
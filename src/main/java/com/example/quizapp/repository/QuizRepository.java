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

    /**
     * Finds quizzes by teacher and EAGERLY loads the questions
     * (for teacher/my-quizzes).
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.teacher = :teacher")
    List<Quiz> findByTeacherAndFetchQuestions(@Param("teacher") User teacher);

    /**
     * Finds all quizzes and EAGERLY loads questions AND teacher
     * (for student/quiz-list AND admin/manage-quizzes).
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions LEFT JOIN FETCH q.teacher")
    List<Quiz> findAllAndFetchQuestionsAndTeacher();

    /**
     * Finds a single quiz by ID and EAGERLY loads questions AND options
     * (for student/take-quiz and student/submit-quiz).
     */
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions qn LEFT JOIN FETCH qn.options WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestionsAndOptions(@Param("id") Long id);

    /**
     * Finds a single quiz by ID and EAGERLY loads questions
     * (for teacher/quiz-results).
     */
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestions(@Param("id") Long id);
}
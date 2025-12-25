package com.example.quizapp.repository;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // FIX: Added LEFT JOIN FETCH q.classroom to prevent LazyInitializationException
    @Query(value = "SELECT q FROM Quiz q LEFT JOIN FETCH q.teacher LEFT JOIN FETCH q.classroom",
            countQuery = "SELECT COUNT(q) FROM Quiz q")
    Page<Quiz> findAllAndFetchTeacher(Pageable pageable);

    // FIX: Added LEFT JOIN FETCH q.classroom here too
    @Query(value = "SELECT q FROM Quiz q LEFT JOIN FETCH q.teacher LEFT JOIN FETCH q.classroom " +
            "WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(q.subject) LIKE LOWER(CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(q) FROM Quiz q WHERE LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(q.subject) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Quiz> searchQuizzes(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT q FROM Quiz q LEFT JOIN FETCH q.teacher WHERE q.classroom.id = :classId",
            countQuery = "SELECT COUNT(q) FROM Quiz q WHERE q.classroom.id = :classId")
    Page<Quiz> findByClassroomId(@Param("classId") Long classId, Pageable pageable);

    @Query("SELECT q FROM Quiz q " +
            "LEFT JOIN FETCH q.questions qn " +
            "LEFT JOIN FETCH qn.options " +
            "LEFT JOIN FETCH q.classroom " +  // <--- ADD THIS LINE
            "WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestionsAndOptions(@Param("id") Long id);

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :id")
    Optional<Quiz> findByIdAndFetchQuestions(@Param("id") Long id);

    @Query(value = "SELECT q FROM Quiz q LEFT JOIN FETCH q.teacher WHERE q.classroom.id = :classId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(q) FROM Quiz q WHERE q.classroom.id = :classId " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Quiz> searchByClassroomId(@Param("classId") Long classId, @Param("search") String search, Pageable pageable);

    @Query("SELECT q FROM Quiz q WHERE q.classroom.id = :classId " +
            "AND (:showAll = true OR q.published = true) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY q.id DESC")
    List<Quiz> findForStudent(@Param("classId") Long classId,
                              @Param("search") String search,
                              @Param("showAll") boolean showAll);


}
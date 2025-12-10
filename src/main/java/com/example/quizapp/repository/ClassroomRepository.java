package com.example.quizapp.repository;

import com.example.quizapp.model.Classroom;
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
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findByTeacherId(Long teacherId);

    Optional<Classroom> findByCode(String code);

    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.students WHERE c.code = :code")
    Optional<Classroom> findByCodeAndFetchStudents(@Param("code") String code);

    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.quizzes LEFT JOIN FETCH c.teacher WHERE c.id = :id")
    Optional<Classroom> findByIdAndFetchQuizzes(@Param("id") Long id);

    @Query("SELECT c FROM Classroom c JOIN c.students s LEFT JOIN FETCH c.teacher WHERE s = :student")
    List<Classroom> findByStudent(@Param("student") User student);

    // Pagination for Admin List
    @Query(value = "SELECT c FROM Classroom c LEFT JOIN FETCH c.teacher",
            countQuery = "SELECT COUNT(c) FROM Classroom c")
    Page<Classroom> findAllAndFetchTeacher(Pageable pageable);

    // Search for Admin List
    @Query(value = "SELECT c FROM Classroom c LEFT JOIN FETCH c.teacher " +
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(c) FROM Classroom c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Classroom> searchClassrooms(@Param("search") String search, Pageable pageable);

    /**
     * FIX: This method is CRITICAL for the Create Quiz page.
     * It loads all classes AND their teachers in one query.
     */
    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.teacher")
    List<Classroom> findAllWithTeachers();
}
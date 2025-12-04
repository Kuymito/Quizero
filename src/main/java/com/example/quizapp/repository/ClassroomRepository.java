package com.example.quizapp.repository;

import com.example.quizapp.model.Classroom;
import com.example.quizapp.model.User;
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

    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.teacher")
    List<Classroom> findAllAndFetchTeacher();

    // NEW: Search classes by Name or Code
    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.teacher " +
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Classroom> searchClassrooms(@Param("search") String search);
}
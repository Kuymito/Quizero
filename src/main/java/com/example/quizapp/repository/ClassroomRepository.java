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

    /**
     * FIX 1: Fetch 'students' eagerly.
     * Required for the 'joinClass' method to work without crashing.
     */
    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.students WHERE c.code = :code")
    Optional<Classroom> findByCodeAndFetchStudents(@Param("code") String code);

    /**
     * FIX 2: Fetch 'quizzes' and 'teacher' eagerly.
     * Required for the 'Class Details' page.
     */
    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.quizzes LEFT JOIN FETCH c.teacher WHERE c.id = :id")
    Optional<Classroom> findByIdAndFetchQuizzes(@Param("id") Long id);

    /**
     * FIX 3: Fetch 'teacher' eagerly.
     * Required for the 'Student Dashboard' to display the teacher's name.
     */
    @Query("SELECT c FROM Classroom c JOIN c.students s LEFT JOIN FETCH c.teacher WHERE s = :student")
    List<Classroom> findByStudent(@Param("student") User student);

    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.teacher")
    List<Classroom> findAllAndFetchTeacher();
}
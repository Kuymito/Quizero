package com.example.quizapp.repository;

import com.example.quizapp.model.User;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // FIX: Return Page<User> and accept Pageable
    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(String username, String fullName, Pageable pageable);

    List<User> findByRole(User.Role role);
}
package com.example.quizapp;

import com.example.quizapp.model.User;
import com.example.quizapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class QuizAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizAppApplication.class, args);
    }

    /**
     * Creates test users with simplified usernames.
     * All users have the password: "password123"
     */
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // FIX: Changed username from "templates/admin" to "admin"
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("password123"));
                admin.setRole(User.Role.ROLE_ADMIN);
                admin.setFullName("Admin User");
                userRepository.save(admin);
                System.out.println("Created ADMIN user: admin / password123");
            }

            // This one was already correct
            if (userRepository.findByUsername("teacher").isEmpty()) {
                User teacher = new User();
                teacher.setUsername("teacher");
                teacher.setPassword(passwordEncoder.encode("password123"));
                teacher.setRole(User.Role.ROLE_TEACHER);
                teacher.setFullName("Teacher User");
                userRepository.save(teacher);
                System.out.println("Created TEACHER user: teacher / password123");
            }

            // FIX: Changed username from "templates/student" to "student"
            if (userRepository.findByUsername("student").isEmpty()) {
                User student = new User();
                student.setUsername("student");
                student.setPassword(passwordEncoder.encode("password123"));
                student.setRole(User.Role.ROLE_STUDENT);
                student.setFullName("Student User");
                userRepository.save(student);
                System.out.println("Created STUDENT user: student / password123");
            }
        };
    }
}
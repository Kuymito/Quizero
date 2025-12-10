package com.example.quizapp;

import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class QuizAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizAppApplication.class, args);
    }

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            ClassroomRepository classroomRepository,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Check if data exists to prevent duplicates on restart
            if (userRepository.count() > 0) {
                System.out.println("Database already seeded. Skipping initialization.");
                return;
            }

            System.out.println("Seeding database with real IT data...");

            // --- 1. CREATE USERS ---
            User admin = createUser(userRepository, "admin", "password123", "System Administrator", User.Role.ROLE_ADMIN, passwordEncoder);

            // Teachers
            User teacherAlice = createUser(userRepository, "alice", "password123", "Alice Johnson", User.Role.ROLE_TEACHER, passwordEncoder);
            User teacherDavid = createUser(userRepository, "david", "password123", "David Lee", User.Role.ROLE_TEACHER, passwordEncoder);

            // Students
            User studentBob = createUser(userRepository, "bob", "password123", "Bob Smith", User.Role.ROLE_STUDENT, passwordEncoder);
            User studentCharlie = createUser(userRepository, "charlie", "password123", "Charlie Davis", User.Role.ROLE_STUDENT, passwordEncoder);
            User studentEve = createUser(userRepository, "eve", "password123", "Eve Wilson", User.Role.ROLE_STUDENT, passwordEncoder);

            // --- 2. CREATE CLASSES ---

            // Class 1: Java Programming (Teacher: Alice)
            Classroom javaClass = createClassroom(classroomRepository, "CS101: Java Programming", teacherAlice);
            enrollStudents(classroomRepository, javaClass, List.of(studentBob, studentCharlie));

            // Class 2: Web Development (Teacher: David)
            Classroom webClass = createClassroom(classroomRepository, "WEB200: Frontend Development", teacherDavid);
            enrollStudents(classroomRepository, webClass, List.of(studentBob, studentEve));

            // Class 3: Database Systems (Teacher: Alice)
            Classroom dbClass = createClassroom(classroomRepository, "DB300: Database Systems", teacherAlice);
            enrollStudents(classroomRepository, dbClass, List.of(studentCharlie, studentEve));


            // --- 3. CREATE QUIZZES & QUESTIONS ---

            // == Quiz 1: Java Basics (for CS101) ==
            Quiz javaQuiz1 = createQuiz(quizRepository, "Java Fundamentals", "Programming", teacherAlice, javaClass);

            createQuestion(questionRepository, optionRepository, javaQuiz1,
                    "Which keyword is used to define a class in Java?",
                    new String[]{"struct", "class", "object", "define"}, 1); // Correct: class

            createQuestion(questionRepository, optionRepository, javaQuiz1,
                    "What is the size of an int variable in Java?",
                    new String[]{"8 bit", "16 bit", "32 bit", "64 bit"}, 2); // Correct: 32 bit

            createQuestion(questionRepository, optionRepository, javaQuiz1,
                    "Which method is the entry point of a Java application?",
                    new String[]{"start()", "run()", "main()", "init()"}, 2); // Correct: main()

            // == Quiz 2: OOP Concepts (for CS101) ==
            Quiz javaQuiz2 = createQuiz(quizRepository, "OOP Concepts", "Programming", teacherAlice, javaClass);

            createQuestion(questionRepository, optionRepository, javaQuiz2,
                    "Which principle allows a subclass to provide a specific implementation of a method?",
                    new String[]{"Encapsulation", "Polymorphism", "Abstraction", "Inheritance"}, 1); // Polymorphism

            createQuestion(questionRepository, optionRepository, javaQuiz2,
                    "Which access modifier makes a member visible only within its own class?",
                    new String[]{"public", "protected", "default", "private"}, 3); // private

            // == Quiz 3: HTML & CSS (for WEB200) ==
            Quiz webQuiz1 = createQuiz(quizRepository, "HTML & CSS Mastery", "Web Development", teacherDavid, webClass);

            createQuestion(questionRepository, optionRepository, webQuiz1,
                    "What does HTML stand for?",
                    new String[]{"Hyper Text Markup Language", "High Tech Multi Language", "Hyperlink Text Management Language", "Home Tool Markup Language"}, 0);

            createQuestion(questionRepository, optionRepository, webQuiz1,
                    "Which property is used to change the background color?",
                    new String[]{"color", "bgcolor", "background-color", "bg-color"}, 2);

            createQuestion(questionRepository, optionRepository, webQuiz1,
                    "Which HTML tag is used to define an internal style sheet?",
                    new String[]{"<css>", "<script>", "<style>", "<link>"}, 2);

            // == Quiz 4: SQL Basics (for DB300) ==
            Quiz dbQuiz1 = createQuiz(quizRepository, "SQL Essentials", "Databases", teacherAlice, dbClass);

            createQuestion(questionRepository, optionRepository, dbQuiz1,
                    "Which SQL statement is used to extract data from a database?",
                    new String[]{"GET", "OPEN", "EXTRACT", "SELECT"}, 3);

            createQuestion(questionRepository, optionRepository, dbQuiz1,
                    "Which keyword is used to filter records?",
                    new String[]{"WHERE", "FILTER", "SEARCH", "WHEN"}, 0);

            System.out.println("Database seeding completed successfully!");
        };
    }

    // Helper Methods to keep code clean

    private User createUser(UserRepository repo, String username, String password, String fullname, User.Role role, PasswordEncoder encoder) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setFullName(fullname);
        user.setRole(role);
        user.setEnabled(true);
        return repo.save(user);
    }

    private Classroom createClassroom(ClassroomRepository repo, String name, User teacher) {
        Classroom cls = new Classroom();
        cls.setName(name);
        cls.setTeacher(teacher);
        // Code is auto-generated in the Entity or you can set it here if you removed @PrePersist
        return repo.save(cls);
    }

    private void enrollStudents(ClassroomRepository repo, Classroom classroom, List<User> students) {
        classroom.getStudents().addAll(students);
        repo.save(classroom);
    }

    private Quiz createQuiz(QuizRepository repo, String title, String subject, User teacher, Classroom classroom) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setSubject(subject);
        quiz.setTeacher(teacher);
        quiz.setCreator(teacher);
        quiz.setClassroom(classroom);
        return repo.save(quiz);
    }

    private void createQuestion(QuestionRepository qRepo, OptionRepository oRepo, Quiz quiz, String text, String[] optionsText, int correctIndex) {
        Question q = new Question();
        q.setText(text);
        q.setQuiz(quiz);
        qRepo.save(q);

        List<Option> options = new ArrayList<>();
        for (int i = 0; i < optionsText.length; i++) {
            Option o = new Option();
            o.setText(optionsText[i]);
            o.setCorrect(i == correctIndex);
            o.setQuestion(q);
            options.add(o);
        }
        oRepo.saveAll(options);
    }
}
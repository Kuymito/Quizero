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
import java.util.Random;

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
            // Prevent duplicate seeding on restart
            if (userRepository.count() > 10) {
                System.out.println("Database already seeded. Skipping...");
                return;
            }

            System.out.println("Starting high-fidelity data seeding...");

            // 1. CORE USERS
            User admin = createUser(userRepository, "admin", "password123", "System Admin", User.Role.ROLE_ADMIN, passwordEncoder);
            User teacherAlice = createUser(userRepository, "alice", "password123", "Alice Johnson", User.Role.ROLE_TEACHER, passwordEncoder);
            User teacherDavid = createUser(userRepository, "david", "password123", "David Lee", User.Role.ROLE_TEACHER, passwordEncoder);
            User studentBob = createUser(userRepository, "bob", "password123", "Bob Smith", User.Role.ROLE_STUDENT, passwordEncoder);

            // 2. CLASSROOMS
            Classroom javaClass = createClassroom(classroomRepository, "CS101: Java Programming", teacherAlice);
            enrollStudent(classroomRepository, javaClass, studentBob);

            Classroom webClass = createClassroom(classroomRepository, "WEB200: Web Development", teacherDavid);
            enrollStudent(classroomRepository, webClass, studentBob);

            // 3. ALICE'S 20 REAL QUIZZES
            System.out.println("Generating 20 real Java quizzes for Alice...");
            seedAliceQuizzes(quizRepository, questionRepository, optionRepository, teacherAlice, javaClass);

            // 4. BULK DATA (Keeping your original logic for testing pagination)
            System.out.println("Generating bulk data for testing...");
            Random rand = new Random();
            List<User> bulkStudents = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                bulkStudents.add(createUser(userRepository, "student" + i, "password", "Student " + i, User.Role.ROLE_STUDENT, passwordEncoder));
            }

            String[] subjects = {"History", "Math", "Physics", "Chemistry", "Biology"};
            for (int i = 1; i <= 10; i++) {
                String subject = subjects[rand.nextInt(subjects.length)];
                Classroom cls = createClassroom(classroomRepository, subject + " " + (100 + i), teacherDavid);

                Quiz bulkQuiz = new Quiz();
                bulkQuiz.setTitle(subject + " Quiz " + i);
                bulkQuiz.setSubject(cls.getName());
                bulkQuiz.setTeacher(teacherDavid);
                bulkQuiz.setClassroom(cls);
                quizRepository.save(bulkQuiz);

                createQuestion(questionRepository, optionRepository, bulkQuiz, "Bulk Question for " + subject, new String[]{"Ans A", "Ans B", "Ans C", "Ans D"}, 0);
            }

            System.out.println("Data seeding complete! Alice now has a full Java curriculum.");
        };
    }

    private void seedAliceQuizzes(QuizRepository qr, QuestionRepository qRepo, OptionRepository oRepo, User teacher, Classroom classroom) {
        // Data structure: {Title, Question, Option1, Option2, Option3, Option4, CorrectIndex}
        String[][] quizData = {
                {"Java Basics", "Which of these is not a primitive type?", "int", "boolean", "String", "char", "2"},
                {"JVM Internals", "Which component is responsible for converting bytecode to machine code?", "ClassLoader", "JIT Compiler", "Garbage Collector", "JRE", "1"},
                {"OOP Principles", "Which principle focuses on hiding internal state?", "Inheritance", "Polymorphism", "Encapsulation", "Abstraction", "2"},
                {"Data Types", "What is the size of an 'int' in Java?", "16-bit", "32-bit", "64-bit", "8-bit", "1"},
                {"Control Flow", "Which loop is guaranteed to execute at least once?", "for", "while", "do-while", "foreach", "2"},
                {"String Pool", "Where are String literals stored in memory?", "Heap", "Stack", "String Constant Pool", "Registers", "2"},
                {"Arrays", "How do you get the length of an array 'arr'?", "arr.size()", "arr.length", "arr.length()", "arr.count", "1"},
                {"Inheritance", "Which keyword is used to inherit a class?", "implements", "instanceof", "extends", "super", "2"},
                {"Interfaces", "Can a class implement multiple interfaces?", "Yes", "No", "Only if abstract", "Only in Java 17+", "0"},
                {"Exception Handling", "Which block always executes after try-catch?", "finally", "catch", "throw", "stop", "0"},
                {"Collections", "Which collection does not allow duplicate elements?", "List", "Set", "Map", "Vector", "1"},
                {"ArrayList vs LinkedList", "Which is better for frequent deletions?", "ArrayList", "LinkedList", "Vector", "Stack", "1"},
                {"HashMap", "What is the default initial capacity of HashMap?", "8", "12", "16", "32", "2"},
                {"Generics", "What does 'T' usually stand for in Generics?", "Type", "Time", "Total", "Template", "0"},
                {"Multithreading", "How do you start a thread?", "thread.run()", "thread.start()", "thread.execute()", "thread.begin()", "1"},
                {"Lambda Expressions", "Which version introduced Lambdas?", "Java 7", "Java 8", "Java 11", "Java 17", "1"},
                {"Streams API", "Which method is a terminal operation?", "filter()", "map()", "collect()", "sorted()", "2"},
                {"Maven Basics", "What is the main configuration file in Maven?", "maven.xml", "settings.json", "pom.xml", "build.gradle", "2"},
                {"Spring Boot", "Which annotation starts a Spring Boot app?", "@SpringBootApplication", "@EnableAutoConfiguration", "@Component", "@Controller", "0"},
                {"Java 21", "Which feature was finalized in Java 21?", "Records", "Virtual Threads", "Lambdas", "Modules", "1"}
        };

        for (String[] data : quizData) {
            Quiz quiz = new Quiz();
            quiz.setTitle(data[0]);
            quiz.setSubject(classroom.getName());
            quiz.setTeacher(teacher);
            quiz.setCreator(teacher);
            quiz.setClassroom(classroom);
            qr.save(quiz);

            createQuestion(qRepo, oRepo, quiz, data[1], new String[]{data[2], data[3], data[4], data[5]}, Integer.parseInt(data[6]));
        }
    }

    // --- HELPER METHODS ---

    private User createUser(UserRepository repo, String username, String password, String fullname, User.Role role, PasswordEncoder encoder) {
        return repo.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(encoder.encode(password));
            user.setFullName(fullname);
            user.setRole(role);
            user.setEnabled(true);
            return repo.save(user);
        });
    }

    private Classroom createClassroom(ClassroomRepository repo, String name, User teacher) {
        Classroom cls = new Classroom();
        cls.setName(name);
        cls.setTeacher(teacher);
        return repo.save(cls);
    }

    private void enrollStudent(ClassroomRepository repo, Classroom classroom, User student) {
        if (!classroom.getStudents().contains(student)) {
            classroom.getStudents().add(student);
            repo.save(classroom);
        }
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
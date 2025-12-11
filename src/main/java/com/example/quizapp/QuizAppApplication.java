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
                return;
            }

            System.out.println("Starting massive data seeding...");

            // ==========================================
            // 1. CORE REAL DATA (For Demo)
            // ==========================================

            // Users
            User admin = createUser(userRepository, "admin", "password123", "System Admin", User.Role.ROLE_ADMIN, passwordEncoder);
            User teacherAlice = createUser(userRepository, "alice", "password123", "Alice Johnson", User.Role.ROLE_TEACHER, passwordEncoder);
            User teacherDavid = createUser(userRepository, "david", "password123", "David Lee", User.Role.ROLE_TEACHER, passwordEncoder);
            User studentBob = createUser(userRepository, "bob", "password123", "Bob Smith", User.Role.ROLE_STUDENT, passwordEncoder);

            // Classes
            Classroom javaClass = createClassroom(classroomRepository, "CS101: Java Programming", teacherAlice);
            enrollStudent(classroomRepository, javaClass, studentBob);

            Classroom webClass = createClassroom(classroomRepository, "WEB200: Web Development", teacherDavid);
            enrollStudent(classroomRepository, webClass, studentBob);

            // Real Quizzes
            createRealJavaQuiz(quizRepository, questionRepository, optionRepository, teacherAlice, javaClass);
            createRealWebQuiz(quizRepository, questionRepository, optionRepository, teacherDavid, webClass);


            // ==========================================
            // 2. BULK DATA (To Trigger Pagination)
            // ==========================================

            System.out.println("Generating bulk data for pagination...");
            Random rand = new Random();

            // A. Generate 50 Students
            List<User> bulkStudents = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                User s = createUser(userRepository, "student" + i, "password", "Student " + i + " Surname", User.Role.ROLE_STUDENT, passwordEncoder);
                bulkStudents.add(s);
            }

            // B. Generate 10 Extra Teachers
            List<User> bulkTeachers = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                User t = createUser(userRepository, "teacher" + i, "password", "Teacher " + i + " Roberts", User.Role.ROLE_TEACHER, passwordEncoder);
                bulkTeachers.add(t);
            }
            bulkTeachers.add(teacherAlice);
            bulkTeachers.add(teacherDavid);

            // C. Generate 20 Extra Classes
            List<Classroom> bulkClasses = new ArrayList<>();
            String[] subjects = {"History", "Math", "Physics", "Chemistry", "Biology", "Literature", "Economics"};

            for (int i = 1; i <= 20; i++) {
                User randomTeacher = bulkTeachers.get(rand.nextInt(bulkTeachers.size()));
                String subject = subjects[rand.nextInt(subjects.length)];
                Classroom cls = createClassroom(classroomRepository, subject + " " + (100 + i) + ": Advanced " + subject, randomTeacher);

                // Enroll 5-10 random students per class
                for(int j=0; j<5; j++) {
                    enrollStudent(classroomRepository, cls, bulkStudents.get(rand.nextInt(bulkStudents.size())));
                }
                bulkClasses.add(cls);
            }
            bulkClasses.add(javaClass);
            bulkClasses.add(webClass);

            // D. Generate 60 Extra Quizzes
            for (int i = 1; i <= 60; i++) {
                Classroom randomClass = bulkClasses.get(rand.nextInt(bulkClasses.size()));
                User teacher = randomClass.getTeacher();

                Quiz quiz = new Quiz();
                quiz.setTitle("Quiz #" + i + ": " + randomClass.getName() + " Review");
                // FIX: Match subject to Class Name automatically
                quiz.setSubject(randomClass.getName());
                quiz.setTeacher(teacher);
                quiz.setCreator(teacher);
                quiz.setClassroom(randomClass);
                quizRepository.save(quiz);

                // Add 2 Dummy Questions per Quiz
                createQuestion(questionRepository, optionRepository, quiz, "Is this a bulk generated question?", new String[]{"Yes", "No", "Maybe", "Unknown"}, 0);
                createQuestion(questionRepository, optionRepository, quiz, "What is the answer to question " + i + "?", new String[]{"A", "B", "C", "D"}, 2);
            }

            System.out.println("Data seeding complete! You can now test pagination.");
        };
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

    private void createRealJavaQuiz(QuizRepository qr, QuestionRepository qRepo, OptionRepository oRepo, User teacher, Classroom classroom) {
        Quiz quiz = new Quiz();
        quiz.setTitle("Java Fundamentals");
        quiz.setSubject(classroom.getName()); // Match Class Name
        quiz.setTeacher(teacher);
        quiz.setCreator(teacher);
        quiz.setClassroom(classroom);
        qr.save(quiz);

        createQuestion(qRepo, oRepo, quiz, "Which keyword creates an object?", new String[]{"class", "new", "object", "create"}, 1);
        createQuestion(qRepo, oRepo, quiz, "int size in Java?", new String[]{"16 bit", "32 bit", "64 bit", "8 bit"}, 1);
    }

    private void createRealWebQuiz(QuizRepository qr, QuestionRepository qRepo, OptionRepository oRepo, User teacher, Classroom classroom) {
        Quiz quiz = new Quiz();
        quiz.setTitle("HTML Basics");
        quiz.setSubject(classroom.getName()); // Match Class Name
        quiz.setTeacher(teacher);
        quiz.setCreator(teacher);
        quiz.setClassroom(classroom);
        qr.save(quiz);

        createQuestion(qRepo, oRepo, quiz, "HTML stands for?", new String[]{"Hyper Text Markup Language", "High Tech Language", "Hyperlinks Text", "Home Tool Language"}, 0);
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
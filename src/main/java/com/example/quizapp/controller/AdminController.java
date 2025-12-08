package com.example.quizapp.controller;

import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder; // Import needed
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private PasswordEncoder passwordEncoder; // needed for user updates

    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    // --- USERS ---
    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "search", required = false) String search, Model model) {
        List<User> users;
        if (search != null && !search.isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search);
        } else {
            users = userRepository.findAll();
        }
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        return "admin/manage-users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        model.addAttribute("user", user);
        return "admin/edit-user";
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestParam("fullName") String fullName,
                             @RequestParam("username") String username,
                             @RequestParam("role") String role,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        user.setFullName(fullName);
        user.setUsername(username);
        user.setRole(User.Role.valueOf(role));

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "User updated successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String loggedInUsername = authentication.getName();
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        if (userToDelete.getUsername().equals(loggedInUsername)) {
            redirectAttributes.addFlashAttribute("error", "Error: You cannot delete your own account.");
        } else {
            // SOFT DELETE FIX:
            // Instead of userRepository.deleteById(id), we just disable them.
            userToDelete.setEnabled(false);
            userRepository.save(userToDelete);

            redirectAttributes.addFlashAttribute("success", "User '" + userToDelete.getUsername() + "' has been disabled.");
        }

        return "redirect:/admin/users";
    }

    // --- QUIZZES ---
    @GetMapping("/quizzes")
    public String manageQuizzes(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Quiz> quizzes;
        if (search != null && !search.isEmpty()) {
            quizzes = quizRepository.searchQuizzes(search);
        } else {
            quizzes = quizRepository.findAllAndFetchQuestionsAndTeacher();
        }
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("search", search);
        return "admin/manage-quizzes";
    }

    @GetMapping("/quiz/edit/{id}")
    public String editQuizForm(@PathVariable Long id, Model model) {
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "admin/edit-quiz";
    }

    @PostMapping("/quiz/update/{id}")
    public String updateQuiz(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        quiz.setTitle(request.getParameter("quizTitle"));
        quiz.setSubject(request.getParameter("quizSubject"));

        for (Question question : quiz.getQuestions()) {
            String qTextParam = request.getParameter("question_" + question.getId());
            if (qTextParam != null) question.setText(qTextParam);

            String selectedOptionIdStr = request.getParameter("correct_option_" + question.getId());
            Long correctOptionId = (selectedOptionIdStr != null) ? Long.parseLong(selectedOptionIdStr) : -1L;

            for (Option option : question.getOptions()) {
                String oTextParam = request.getParameter("option_" + option.getId());
                if (oTextParam != null) option.setText(oTextParam);
                option.setCorrect(option.getId().equals(correctOptionId));
            }
        }
        quizRepository.save(quiz);
        redirectAttributes.addFlashAttribute("success", "Quiz updated successfully.");
        return "redirect:/admin/quizzes";
    }

    @GetMapping("/quiz/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz);
        quizAttemptRepository.deleteAll(attempts);
        quizRepository.delete(quiz);
        redirectAttributes.addFlashAttribute("success", "Quiz '" + quiz.getTitle() + "' deleted successfully.");
        return "redirect:/admin/quizzes";
    }

    // --- CLASSES ---
    @GetMapping("/classes")
    public String manageClasses(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Classroom> classes;
        if (search != null && !search.isEmpty()) {
            classes = classroomRepository.searchClassrooms(search);
        } else {
            classes = classroomRepository.findAllAndFetchTeacher();
        }
        model.addAttribute("classes", classes);
        model.addAttribute("search", search);
        return "admin/manage-classes";
    }

    @GetMapping("/class/edit/{id}")
    public String editClassForm(@PathVariable Long id, Model model) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
        model.addAttribute("classroom", classroom);
        return "admin/edit-class";
    }

    @PostMapping("/class/update/{id}")
    public String updateClass(@PathVariable Long id, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
        classroom.setName(name);
        classroomRepository.save(classroom);
        redirectAttributes.addFlashAttribute("success", "Class updated successfully.");
        return "redirect:/admin/classes";
    }

    @GetMapping("/class/delete/{id}")
    public String deleteClass(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        classroomRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Class deleted successfully.");
        return "redirect:/admin/classes";
    }

    @GetMapping("/users/new")
    public String createUserForm(Model model) {
        // We pass a new empty User object to the form
        model.addAttribute("user", new User());
        return "admin/create-user";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam("fullName") String fullName,
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("role") String role,
                             RedirectAttributes redirectAttributes) {

        // 1. Check if username exists
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username '" + username + "' already exists.");
            return "redirect:/admin/users/new";
        }

        // 2. Create new user
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // Hash the password
        newUser.setRole(User.Role.valueOf(role));
        newUser.setEnabled(true);

        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("success", "User created successfully!");
        return "redirect:/admin/users";
    }
}
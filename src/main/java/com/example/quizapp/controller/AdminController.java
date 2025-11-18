package com.example.quizapp.controller;

import com.example.quizapp.model.Quiz;
import com.example.quizapp.model.User;
import com.example.quizapp.repository.QuizRepository;
import com.example.quizapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizRepository quizRepository;

    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/manage-users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String loggedInUsername = authentication.getName();
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        if (userToDelete.getUsername().equals(loggedInUsername)) {
            redirectAttributes.addFlashAttribute("error", "Error: You cannot delete your own account.");
        } else {
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "User '" + userToDelete.getUsername() + "' deleted successfully.");
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/quizzes")
    public String manageQuizzes(Model model) {
        List<Quiz> quizzes = quizRepository.findAll();
        model.addAttribute("quizzes", quizzes);
        return "admin/manage-quizzes";
    }

    /**
     * NEW: Allows an Admin to delete any quiz.
     */
    @GetMapping("/quiz/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        quizRepository.delete(quiz);
        redirectAttributes.addFlashAttribute("success", "Quiz '" + quiz.getTitle() + "' deleted successfully.");
        return "redirect:/admin/quizzes";
    }
}
package com.example.quizapp.controller;

import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Import Objects for comparison

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    // NEW: Added repository to fetch quiz attempts
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @GetMapping("/dashboard")
    public String teacherDashboard() {
        return "teacher/dashboard";
    }

    @GetMapping("/quizzes")
    public String listQuizzes(Model model) {
        // FIX: Use the new EAGER fetch method to load questions
        List<Quiz> quizzes = quizRepository.findAllAndFetchQuestions();
        model.addAttribute("quizzes", quizzes);
        return "student/quiz-list";
    }

    /**
     * Displays the specific quiz for the student to take.
     */
    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model) {
        // FIX: Use the new EAGER fetch method to load questions
        Quiz quiz = quizRepository.findByIdAndFetchQuestions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @GetMapping("/quiz/new")
    public String createQuizForm(Model model) {
        return "teacher/create-quiz";
    }

    @PostMapping("/quiz/create")
    public String createQuiz(HttpServletRequest request, Authentication authentication) {
        User teacher = getLoggedInUser(authentication);

        Quiz quiz = new Quiz();
        quiz.setTitle(request.getParameter("quizTitle"));
        quiz.setSubject(request.getParameter("quizSubject"));
        quiz.setTeacher(teacher);
        quiz.setCreator(teacher);
        Quiz savedQuiz = quizRepository.save(quiz);

        Map<String, String[]> parameterMap = request.getParameterMap();
        int questionIndex = 0;
        while (parameterMap.containsKey("questions[" + questionIndex + "].text")) {
            Question question = new Question();
            question.setText(parameterMap.get("questions[" + questionIndex + "].text")[0]);
            question.setQuiz(savedQuiz);
            Question savedQuestion = questionRepository.save(question);

            List<Option> options = new ArrayList<>();
            int optionIndex = 0;
            while (parameterMap.containsKey("questions[" + questionIndex + "].options[" + optionIndex + "].text")) {
                Option option = new Option();
                option.setText(parameterMap.get("questions[" + questionIndex + "].options[" + optionIndex + "].text")[0]);
                String correctOptionValue = parameterMap.get("questions[" + questionIndex + "].correctOption")[0];
                option.setCorrect(correctOptionValue.equals(String.valueOf(optionIndex)));
                option.setQuestion(savedQuestion);
                options.add(option);
                optionIndex++;
            }
            optionRepository.saveAll(options);
            questionIndex++;
        }

        return "redirect:/teacher/quizzes";
    }

    /**
     * NEW: Allows a Teacher to delete one of their own quizzes.
     */
    @GetMapping("/quiz/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        // Security Check: Ensure the teacher owns this quiz
        if (!Objects.equals(quiz.getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only delete your own quizzes.");
            return "redirect:/teacher/quizzes";
        }

        quizRepository.delete(quiz);
        redirectAttributes.addFlashAttribute("success", "Quiz '" + quiz.getTitle() + "' deleted successfully.");
        return "redirect:/teacher/quizzes";
    }

    /**
     * NEW: Allows a Teacher to see all attempts for a specific quiz.
     */
    @GetMapping("/quiz/results/{id}")
    public String viewQuizResults(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        // Security Check: Ensure the teacher owns this quiz
        if (!Objects.equals(quiz.getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only view results for your own quizzes.");
            return "redirect:/teacher/quizzes";
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz);
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        return "teacher/quiz-results"; // Needs a new template: teacher/quiz-results.html
    }

    private User getLoggedInUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
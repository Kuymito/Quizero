package com.example.quizapp.controller;

import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @GetMapping("/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }

    @GetMapping("/quizzes")
    public String listQuizzes(Model model) {
        List<Quiz> quizzes = quizRepository.findAll();
        model.addAttribute("quizzes", quizzes);
        return "student/quiz-list";
    }

    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @PostMapping("/quiz/submit")
    public String submitQuiz(@RequestParam("quizId") Long quizId,
                             HttpServletRequest request,
                             Authentication authentication) {

        User student = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + quizId));

        int score = 0;
        Set<StudentAnswer> studentAnswers = new HashSet<>();
        Set<Question> questions = quiz.getQuestions();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setTotalQuestions(questions.size());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        for (Question question : questions) {
            String paramName = "question-" + question.getId();
            String selectedOptionIdStr = request.getParameter(paramName);

            if (selectedOptionIdStr != null) {
                Long selectedOptionId = Long.parseLong(selectedOptionIdStr);
                Option selectedOption = optionRepository.findById(selectedOptionId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid option Id:" + selectedOptionId));

                StudentAnswer studentAnswer = new StudentAnswer();
                studentAnswer.setQuizAttempt(savedAttempt);
                studentAnswer.setQuestion(question);
                studentAnswer.setSelectedOption(selectedOption);
                studentAnswers.add(studentAnswer);

                if (selectedOption.isCorrect()) {
                    score++;
                }
            }
        }

        studentAnswerRepository.saveAll(studentAnswers);
        savedAttempt.setStudentAnswers(studentAnswers);
        savedAttempt.setScore(score);
        quizAttemptRepository.save(savedAttempt);

        // Redirect to the new details page for the attempt they just finished
        return "redirect:/student/attempt/" + savedAttempt.getId();
    }

    @GetMapping("/performance")
    public String viewPerformance(Model model, Authentication authentication) {
        User student = getLoggedInUser(authentication);
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudent(student);
        model.addAttribute("attempts", attempts);
        return "student/performance";
    }

    /**
     * NEW: Displays the results of a single quiz attempt.
     */
    @GetMapping("/attempt/{id}")
    public String viewAttemptDetails(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = getLoggedInUser(authentication);
        QuizAttempt attempt = quizAttemptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt Id:" + id));

        // Security Check: Ensure the student owns this attempt
        if (!Objects.equals(attempt.getStudent().getId(), student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only view your own quiz attempts.");
            return "redirect:/student/performance";
        }

        model.addAttribute("attempt", attempt);
        // Note: The 'attempt' object already contains the set of 'studentAnswers'
        // which contains the selected option. You will also need to
        // load the 'question' and its 'options' in the template to show the correct answer.
        return "student/attempt-details"; // Needs a new template: student/attempt-details.html
    }

    private User getLoggedInUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
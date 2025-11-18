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
import java.util.*;
import java.util.stream.Collectors;

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
        // FIX: Use new query to fetch questions AND teacher
        List<Quiz> quizzes = quizRepository.findAllAndFetchQuestionsAndTeacher();
        model.addAttribute("quizzes", quizzes);
        return "student/quiz-list";
    }

    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model) {
        // FIX: Use new query to fetch questions AND options
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @PostMapping("/quiz/submit")
    public String submitQuiz(@RequestParam("quizId") Long quizId,
                             HttpServletRequest request,
                             Authentication authentication) {

        User student = getLoggedInUser(authentication);
        // FIX: Use new query to fetch questions AND options
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + quizId));

        int score = 0;
        Set<StudentAnswer> studentAnswers = new HashSet<>();
        // This Set<Question> is now safe to access
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
                // We can safely find the option now
                Option selectedOption = question.getOptions().stream()
                        .filter(opt -> opt.getId().equals(selectedOptionId))
                        .findFirst()
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

        return "redirect:/student/attempt/" + savedAttempt.getId();
    }

    @GetMapping("/performance")
    public String viewPerformance(Model model, Authentication authentication) {
        User student = getLoggedInUser(authentication);
        // FIX: Use new query to fetch the quiz for each attempt
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentAndFetchQuiz(student);
        model.addAttribute("attempts", attempts);
        return "student/performance";
    }

    @GetMapping("/attempt/{id}")
    public String viewAttemptDetails(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = getLoggedInUser(authentication);
        QuizAttempt attempt = quizAttemptRepository.findByIdAndFetchAllDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt Id:" + id));

        // Security Check
        if (!Objects.equals(attempt.getStudent().getId(), student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only view your own quiz attempts.");
            return "redirect:/student/performance";
        }

        // --- FIX IS HERE ---
        // Create a Map of (Question ID -> StudentAnswer)
        // This moves the complex logic out of Thymeleaf.
        Map<Long, StudentAnswer> studentAnswerMap = attempt.getStudentAnswers().stream()
                .collect(Collectors.toMap(sa -> sa.getQuestion().getId(), sa -> sa));

        model.addAttribute("attempt", attempt);
        model.addAttribute("studentAnswerMap", studentAnswerMap); // <-- ADD THE MAP TO THE MODEL

        return "student/attempt-details";
    }

    private User getLoggedInUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: "+ username));
    }
}
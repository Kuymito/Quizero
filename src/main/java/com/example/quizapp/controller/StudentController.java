package com.example.quizapp.controller;

import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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

    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private StudentAnswerRepository studentAnswerRepository;
    @Autowired private ClassroomRepository classroomRepository;

    @GetMapping("/dashboard")
    public String studentDashboard(Model model, Authentication authentication) {
        User student = getLoggedInUser(authentication);
        List<Classroom> classes = classroomRepository.findByStudent(student);
        model.addAttribute("classes", classes);
        return "student/dashboard";
    }

    // --- JOIN CLASS ---
    @GetMapping("/join")
    public String joinClassForm() {
        return "student/join-class";
    }

    @PostMapping("/join")
    public String joinClass(@RequestParam("code") String code, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = getLoggedInUser(authentication);
        Optional<Classroom> classroomOpt = classroomRepository.findByCodeAndFetchStudents(code);

        if (classroomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid class code.");
            return "redirect:/student/join";
        }

        Classroom classroom = classroomOpt.get();
        classroom.getStudents().add(student);
        classroomRepository.save(classroom);

        redirectAttributes.addFlashAttribute("success", "Joined " + classroom.getName() + " successfully!");
        return "redirect:/student/dashboard";
    }

    // --- VIEW CLASS ---
    @GetMapping("/class/{id}")
    public String viewClass(@PathVariable Long id, Model model, Authentication authentication) {
        User student = getLoggedInUser(authentication);
        Classroom classroom = classroomRepository.findByIdAndFetchQuizzes(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID"));

        model.addAttribute("classroom", classroom);
        return "student/class-details";
    }

    // --- QUIZ METHODS ---

    @GetMapping("/quizzes")
    public String listQuizzes(Model model) {
        List<Quiz> quizzes = quizRepository.findAllAndFetchQuestionsAndTeacher();
        model.addAttribute("quizzes", quizzes);
        return "student/quiz-list";
    }

    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @PostMapping("/quiz/submit")
    public String submitQuiz(@RequestParam("quizId") Long quizId,
                             HttpServletRequest request,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        User student = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(quizId)
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
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentAndFetchQuiz(student);
        model.addAttribute("attempts", attempts);
        return "student/performance";
    }

    @GetMapping("/attempt/{id}")
    public String viewAttemptDetails(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = getLoggedInUser(authentication);
        QuizAttempt attempt = quizAttemptRepository.findByIdAndFetchAllDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt Id:" + id));

        if (!Objects.equals(attempt.getStudent().getId(), student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only view your own quiz attempts.");
            return "redirect:/student/performance";
        }

        Map<Long, StudentAnswer> studentAnswerMap = attempt.getStudentAnswers().stream()
                .collect(Collectors.toMap(sa -> sa.getQuestion().getId(), sa -> sa));

        model.addAttribute("attempt", attempt);
        model.addAttribute("studentAnswerMap", studentAnswerMap);

        return "student/attempt-details";
    }

    // --- LEADERBOARD FEATURES ---

    @GetMapping("/leaderboard")
    public String leaderboardList(Model model) {
        List<Quiz> quizzes = quizRepository.findAllAndFetchQuestionsAndTeacher();
        model.addAttribute("quizzes", quizzes);
        return "student/leaderboard-list";
    }

    @GetMapping("/leaderboard/{quizId}")
    public String leaderboardRankings(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + quizId));

        // FIX: Pass quizId (Long) to match the new repository method signature
        List<QuizAttempt> topAttempts = quizAttemptRepository.findTopAttempts(quizId, PageRequest.of(0, 10));

        model.addAttribute("quiz", quiz);
        model.addAttribute("topAttempts", topAttempts);
        return "student/leaderboard-rankings";
    }

    private User getLoggedInUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
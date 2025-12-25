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

    // ... (Keep dashboard, joinClass methods unchanged) ...

    @GetMapping("/dashboard")
    public String studentDashboard(Model model, Authentication authentication) {
        User student = getLoggedInUser(authentication);
        List<Classroom> classes = classroomRepository.findByStudent(student);
        model.addAttribute("classes", classes);
        return "student/dashboard";
    }

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

    // --- VIEW CLASS (UPDATED) ---
    @GetMapping("/class/{id}")
    public String viewClass(@PathVariable Long id,
                            @RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "showAll", defaultValue = "false") boolean showAll,
                            Model model,
                            Authentication authentication) {

        User student = getLoggedInUser(authentication);

        // FIX: Use the new method that fetches the teacher to prevent LazyInitializationException
        Classroom classroom = classroomRepository.findByIdAndFetchTeacher(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class ID"));

        // Fetch Filtered Quizzes (This list is already passed to the model as 'quizzes')
        List<Quiz> filteredQuizzes = quizRepository.findForStudent(id, search, showAll);

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudent(student);
        Set<Long> attemptedQuizIds = attempts.stream()
                .map(attempt -> attempt.getQuiz().getId())
                .collect(Collectors.toSet());

        model.addAttribute("classroom", classroom);
        model.addAttribute("quizzes", filteredQuizzes);
        model.addAttribute("attemptedQuizIds", attemptedQuizIds);
        model.addAttribute("search", search);
        model.addAttribute("showAll", showAll);

        return "student/class-details";
    }

    // --- QUIZ LIST (UPDATED) ---
    @GetMapping("/quizzes")
    public String listQuizzes(Model model, Authentication authentication) {
        User student = getLoggedInUser(authentication);
        List<Quiz> quizzes = quizRepository.findAllAndFetchQuestionsAndTeacher();


        List<QuizAttempt> attempts = quizAttemptRepository.findByStudent(student);
        Set<Long> attemptedQuizIds = attempts.stream()
                .map(attempt -> attempt.getQuiz().getId())
                .collect(Collectors.toSet());

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("attemptedQuizIds", attemptedQuizIds); // Pass to view
        return "student/quiz-list";
    }



    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        // NEW: Check if quiz is published
        if (!quiz.isPublished()) {
            redirectAttributes.addFlashAttribute("error", "This quiz is currently closed by the teacher.");
            return "redirect:/student/class/" + quiz.getClassroom().getId();
        }

        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @PostMapping("/quiz/submit/{id}")
    public String submitQuiz(@PathVariable("id") Long quizId, HttpServletRequest request, Authentication authentication, RedirectAttributes redirectAttributes) {
        // ... (Use the FIXED logic from the previous turn) ...
        User student = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(quizId).orElseThrow();
        if (!quiz.isPublished()) {
            redirectAttributes.addFlashAttribute("error", "Time's up! The quiz has been closed.");
            return "redirect:/student/class/" + quiz.getClassroom().getId();
        }
        int score = 0;
        Set<StudentAnswer> studentAnswers = new HashSet<>();
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setTotalQuestions(quiz.getQuestions().size());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        for (Question question : quiz.getQuestions()) {
            String[] selectedOptionIds = request.getParameterValues("question_" + question.getId());
            Set<Long> correctOptionIds = question.getOptions().stream().filter(Option::isCorrect).map(Option::getId).collect(Collectors.toSet());
            Set<Long> userSelectedIds = new HashSet<>();
            if (selectedOptionIds != null) {
                for (String optionIdStr : selectedOptionIds) {
                    Long optionId = Long.parseLong(optionIdStr);
                    userSelectedIds.add(optionId);
                    StudentAnswer studentAnswer = new StudentAnswer();
                    studentAnswer.setQuizAttempt(savedAttempt);
                    studentAnswer.setQuestion(question);
                    studentAnswer.setSelectedOption(optionRepository.findById(optionId).orElse(null));
                    studentAnswers.add(studentAnswer);
                }
            }
            if (userSelectedIds.equals(correctOptionIds)) score++;
        }
        studentAnswerRepository.saveAll(studentAnswers);
        savedAttempt.setScore(score);
        quizAttemptRepository.save(savedAttempt);
        return "redirect:/student/attempt/" + savedAttempt.getId();
    }

    // ... (Keep other methods) ...

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
        QuizAttempt attempt = quizAttemptRepository.findByIdAndFetchAllDetails(id).orElseThrow();
        if (!Objects.equals(attempt.getStudent().getId(), student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only view your own quiz attempts.");
            return "redirect:/student/performance";
        }
        Map<Long, StudentAnswer> studentAnswerMap = attempt.getStudentAnswers().stream().collect(Collectors.toMap(sa -> sa.getQuestion().getId(), sa -> sa));
        model.addAttribute("attempt", attempt);
        model.addAttribute("studentAnswerMap", studentAnswerMap);
        return "student/attempt-details";
    }

    @GetMapping("/leaderboard")
    public String leaderboardList(Model model) {
        List<Quiz> quizzes = quizRepository.findAllAndFetchQuestionsAndTeacher();
        model.addAttribute("quizzes", quizzes);
        return "student/leaderboard-list";
    }

    @GetMapping("/leaderboard/{quizId}")
    public String leaderboardRankings(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
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
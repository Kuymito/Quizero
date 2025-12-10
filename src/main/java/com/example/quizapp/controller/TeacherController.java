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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired private QuizRepository quizRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private ClassroomRepository classroomRepository;

    @GetMapping("/dashboard")
    public String teacherDashboard(Model model, Authentication authentication) {
        User teacher = getLoggedInUser(authentication);
        List<Classroom> classes = classroomRepository.findByTeacherId(teacher.getId());
        model.addAttribute("classes", classes);
        return "teacher/dashboard";
    }

    // --- CLASSROOM MANAGEMENT ---

    @GetMapping("/class/new")
    public String createClassForm() {
        return "teacher/create-class";
    }

    @PostMapping("/class/create")
    public String createClass(@RequestParam("name") String name, Authentication authentication) {
        User teacher = getLoggedInUser(authentication);
        Classroom classroom = new Classroom();
        classroom.setName(name);
        classroom.setTeacher(teacher);
        classroomRepository.save(classroom);
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/class/{id}")
    public String manageClass(@PathVariable Long id, Model model, Authentication authentication) {
        User teacher = getLoggedInUser(authentication);
        Classroom classroom = classroomRepository.findByIdAndFetchQuizzes(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Class Id:" + id));

        if (!classroom.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher/dashboard";
        }

        model.addAttribute("classroom", classroom);
        return "teacher/manage-class";
    }

    // --- QUIZ MANAGEMENT ---

    @GetMapping("/class/{classId}/quiz/new")
    public String createQuizForm(@PathVariable Long classId, Model model) {
        model.addAttribute("classId", classId);
        return "teacher/create-quiz";
    }

    @PostMapping("/class/{classId}/quiz/create")
    public String createQuiz(@PathVariable Long classId, HttpServletRequest request, Authentication authentication) throws IOException {
        User teacher = getLoggedInUser(authentication);
        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + classId));

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        Quiz quiz = new Quiz();
        quiz.setTitle(request.getParameter("quizTitle"));
        // FIX: Auto-set Subject to Class Name
        quiz.setSubject(classroom.getName());
        quiz.setTeacher(teacher);
        quiz.setCreator(teacher);
        quiz.setClassroom(classroom);
        Quiz savedQuiz = quizRepository.save(quiz);

        // ... (Keep the rest of the question/option saving logic exactly the same) ...
        // (Copy the question loop logic from your existing TeacherController)
        Map<String, String[]> parameterMap = request.getParameterMap();
        int questionIndex = 0;
        while (parameterMap.containsKey("questions[" + questionIndex + "].text")) {
            Question question = new Question();
            question.setText(parameterMap.get("questions[" + questionIndex + "].text")[0]);
            MultipartFile imageFile = multipartRequest.getFile("questions[" + questionIndex + "].image");
            if (imageFile != null && !imageFile.isEmpty()) {
                question.setImage(imageFile.getBytes());
            }
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

        return "redirect:/teacher/class/" + classId;
    }

    @GetMapping("/quiz/edit/{id}")
    public String editQuizForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        if (!Objects.equals(quiz.getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own quizzes.");
            return "redirect:/teacher/dashboard";
        }

        model.addAttribute("quiz", quiz);
        return "teacher/edit-quiz";
    }

    @PostMapping("/quiz/update/{id}")
    public String updateQuiz(@PathVariable Long id, HttpServletRequest request, Authentication authentication, RedirectAttributes redirectAttributes) throws IOException {
        // ... fetch user/quiz/security check ...
        User teacher = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id).orElseThrow();

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        quiz.setTitle(request.getParameter("quizTitle"));
        // FIX: Ensure subject stays consistent with class
        quiz.setSubject(quiz.getClassroom().getName());

        // ... (Keep the rest of the update logic exactly the same) ...
        for (Question question : quiz.getQuestions()) {
            String qTextParam = request.getParameter("question_" + question.getId());
            if (qTextParam != null) question.setText(qTextParam);

            MultipartFile imageFile = multipartRequest.getFile("question_" + question.getId() + "_image");
            if (imageFile != null && !imageFile.isEmpty()) {
                question.setImage(imageFile.getBytes());
            }
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
        return "redirect:/teacher/class/" + quiz.getClassroom().getId();
    }

    @GetMapping("/quiz/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        if (!Objects.equals(quiz.getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only delete your own quizzes.");
            return "redirect:/teacher/dashboard";
        }

        Long classId = quiz.getClassroom().getId();

        // Delete attempts first
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz);
        quizAttemptRepository.deleteAll(attempts);

        quizRepository.delete(quiz);

        redirectAttributes.addFlashAttribute("success", "Quiz deleted successfully.");
        return "redirect:/teacher/class/" + classId;
    }

    @GetMapping("/quiz/results/{id}")
    public String viewQuizResults(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        User teacher = getLoggedInUser(authentication);
        Quiz quiz = quizRepository.findByIdAndFetchQuestions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        if (!Objects.equals(quiz.getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("error", "Error: You can only view results for your own quizzes.");
            return "redirect:/teacher/dashboard";
        }

        // FIX: Use the new ID-based method
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdAndFetchStudent(quiz.getId());

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        return "teacher/quiz-results";
    }

    private User getLoggedInUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
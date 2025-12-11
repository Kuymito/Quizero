package com.example.quizapp.controller;

import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import
import org.springframework.data.domain.PageRequest; // Import
import org.springframework.data.domain.Pageable; // Import
import org.springframework.data.domain.Sort; // Import
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private OptionRepository optionRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // 1. Fetch Counts for Summary Cards
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(User.Role.ROLE_STUDENT);
        long totalTeachers = userRepository.countByRole(User.Role.ROLE_TEACHER);
        long totalClasses = classroomRepository.count();
        long totalQuizzes = quizRepository.count();
        long totalAttempts = quizAttemptRepository.count();

        // 2. Fetch Recent Activity
        List<QuizAttempt> recentAttempts = quizAttemptRepository.findTop5ByOrderByAttemptedAtDesc();

        // 3. Add to Model
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalTeachers", totalTeachers);
        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("recentAttempts", recentAttempts);

        return "admin/dashboard";
    }

    // --- USERS (With Pagination) ---
    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "search", required = false) String search,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "10") int size,
                              @RequestParam(value = "sortField", defaultValue = "id") String sortField,
                              @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                              Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;
        if (search != null && !search.isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        model.addAttribute("users", userPage.getContent());
        addPaginationAttributes(model, page, userPage, search, sortField, sortDir);
        return "admin/manage-users";
    }

    // ... (keep create/edit/delete User methods) ...
    @GetMapping("/users/new")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/create-user";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam("fullName") String fullName,
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("role") String role,
                             RedirectAttributes redirectAttributes) {
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username '" + username + "' already exists.");
            return "redirect:/admin/users/new";
        }
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(User.Role.valueOf(role));
        newUser.setEnabled(true);
        userRepository.save(newUser);
        redirectAttributes.addFlashAttribute("success", "User created successfully!");
        return "redirect:/admin/users";
    }
    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        model.addAttribute("user", user);
        return "admin/edit-user";
    }
    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id, @RequestParam("fullName") String fullName, @RequestParam("username") String username, @RequestParam("role") String role, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
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
        User userToDelete = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        if (userToDelete.getUsername().equals(loggedInUsername)) {
            redirectAttributes.addFlashAttribute("error", "Error: You cannot delete your own account.");
        } else {
            userToDelete.setEnabled(false);
            userRepository.save(userToDelete);
            redirectAttributes.addFlashAttribute("success", "User '" + userToDelete.getUsername() + "' has been disabled.");
        }
        return "redirect:/admin/users";
    }


    // --- QUIZZES (With Pagination) ---
    @GetMapping("/quizzes")
    public String manageQuizzes(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @RequestParam(value = "sortField", defaultValue = "id") String sortField,
                                @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                                Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Quiz> quizPage;
        if (search != null && !search.isEmpty()) {
            quizPage = quizRepository.searchQuizzes(search, pageable);
        } else {
            quizPage = quizRepository.findAllAndFetchTeacher(pageable);
        }

        model.addAttribute("quizzes", quizPage.getContent());
        addPaginationAttributes(model, page, quizPage, search, sortField, sortDir);
        return "admin/manage-quizzes";
    }

    @GetMapping("/quiz/new")
    public String createQuizForm(Model model) {
        // FIX: Must use findAllWithTeachers() to prevent LazyInitializationException
        List<Classroom> classes = classroomRepository.findAllWithTeachers();

        model.addAttribute("classes", classes);
        return "admin/create-quiz";
    }

    // NEW: Handle Quiz Creation
    @PostMapping("/quiz/create")
    public String createQuiz(@RequestParam("title") String title,
                             // REMOVED: @RequestParam("subject") String subject,
                             @RequestParam("classId") Long classId,
                             HttpServletRequest request,
                             Authentication authentication) throws IOException {

        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Class ID"));
        User adminUser = userRepository.findByUsername(authentication.getName()).orElseThrow();
        User classTeacher = classroom.getTeacher();

        // ... multipart setup ...
        MultipartHttpServletRequest multipartRequest = null;
        if (request instanceof MultipartHttpServletRequest) {
            multipartRequest = (MultipartHttpServletRequest) request;
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        // FIX: Auto-set Subject to Class Name (User doesn't type it anymore)
        quiz.setSubject(classroom.getName());
        quiz.setTeacher(classTeacher);
        quiz.setCreator(adminUser);
        quiz.setClassroom(classroom);
        Quiz savedQuiz = quizRepository.save(quiz);

        // ... (Keep the rest of the question/option saving logic exactly the same) ...
        Map<String, String[]> parameterMap = request.getParameterMap();
        int questionIndex = 0;
        while (parameterMap.containsKey("questions[" + questionIndex + "].text")) {
            // ... copy existing question logic ...
            Question question = new Question();
            question.setText(parameterMap.get("questions[" + questionIndex + "].text")[0]);

            if (multipartRequest != null) {
                MultipartFile imageFile = multipartRequest.getFile("questions[" + questionIndex + "].image");
                if (imageFile != null && !imageFile.isEmpty()) {
                    question.setImage(imageFile.getBytes());
                }
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

        return "redirect:/admin/quizzes";
    }

    // ... (keep edit/delete/update Quiz methods) ...
    @GetMapping("/quiz/edit/{id}")
    public String editQuizForm(@PathVariable Long id, Model model) {
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id).orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "admin/edit-quiz";
    }
    @PostMapping("/quiz/update/{id}")
    public String updateQuiz(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) throws IOException {
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        MultipartHttpServletRequest multipartRequest = null;
        if (request instanceof MultipartHttpServletRequest) {
            multipartRequest = (MultipartHttpServletRequest) request;
        }

        quiz.setTitle(request.getParameter("quizTitle"));
        // FIX: Removed subject update from request. Keep existing or reset to class name.
        quiz.setSubject(quiz.getClassroom().getName());

        // ... (Keep the rest of the update logic exactly the same) ...
        for (Question question : quiz.getQuestions()) {
            // ... copy existing question/option update logic ...
            String qTextParam = request.getParameter("question_" + question.getId());
            if (qTextParam != null) question.setText(qTextParam);

            if (multipartRequest != null) {
                MultipartFile imageFile = multipartRequest.getFile("question_" + question.getId() + "_image");
                if (imageFile != null && !imageFile.isEmpty()) {
                    question.setImage(imageFile.getBytes());
                }
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
        return "redirect:/admin/quizzes";
    }
    @GetMapping("/quiz/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Quiz quiz = quizRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuiz(quiz);
        quizAttemptRepository.deleteAll(attempts);
        quizRepository.delete(quiz);
        redirectAttributes.addFlashAttribute("success", "Quiz '" + quiz.getTitle() + "' deleted successfully.");
        return "redirect:/admin/quizzes";
    }

    // --- CLASSES (With Pagination) ---
    @GetMapping("/classes")
    public String manageClasses(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @RequestParam(value = "sortField", defaultValue = "id") String sortField,
                                @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                                Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Classroom> classPage;
        if (search != null && !search.isEmpty()) {
            classPage = classroomRepository.searchClassrooms(search, pageable);
        } else {
            classPage = classroomRepository.findAllAndFetchTeacher(pageable);
        }

        model.addAttribute("classes", classPage.getContent());
        addPaginationAttributes(model, page, classPage, search, sortField, sortDir);
        return "admin/manage-classes";
    }

    @GetMapping("/class/new")
    public String createClassForm(Model model) {
        // Fetch teachers so Admin can assign one
        List<User> teachers = userRepository.findByRole(User.Role.ROLE_TEACHER);
        model.addAttribute("classroom", new Classroom());
        model.addAttribute("teachers", teachers);
        return "admin/create-class";
    }

    @PostMapping("/class/create")
    public String createClass(@RequestParam("name") String name,
                              @RequestParam("teacherId") Long teacherId,
                              RedirectAttributes redirectAttributes) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Teacher ID"));

        Classroom classroom = new Classroom();
        classroom.setName(name);
        classroom.setTeacher(teacher);
        classroomRepository.save(classroom);

        redirectAttributes.addFlashAttribute("success", "Class created and assigned to " + teacher.getFullName());
        return "redirect:/admin/classes";
    }

    // ... (keep edit/delete/update Class methods) ...
    @GetMapping("/class/edit/{id}")
    public String editClassForm(@PathVariable Long id, Model model) {
        Classroom classroom = classroomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
        model.addAttribute("classroom", classroom);
        return "admin/edit-class";
    }
    @PostMapping("/class/update/{id}")
    public String updateClass(@PathVariable Long id, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        Classroom classroom = classroomRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
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

    private void addPaginationAttributes(Model model, int page, Page<?> pageData, String search, String sortField, String sortDir) {
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
    }
}
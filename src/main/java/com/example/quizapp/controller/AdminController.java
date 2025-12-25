package com.example.quizapp.controller;

// ... (keep existing imports) ...
import com.example.quizapp.model.*;
import com.example.quizapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // ... (Keep dashboard method unchanged) ...
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(User.Role.ROLE_STUDENT);
        long totalTeachers = userRepository.countByRole(User.Role.ROLE_TEACHER);
        long totalClasses = classroomRepository.count();
        long totalQuizzes = quizRepository.count();
        long totalAttempts = quizAttemptRepository.count();
        List<QuizAttempt> recentAttempts = quizAttemptRepository.findRecentAttempts(PageRequest.of(0, 5));

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalTeachers", totalTeachers);
        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("recentAttempts", recentAttempts);

        return "admin/dashboard";
    }

    // --- USERS ---
    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "search", required = false) String search,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "10") int size,
                              @RequestParam(value = "sortField", defaultValue = "id") String sortField,
                              @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                              Model model) {

        if (size < 1) size = 10; // Validation
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;
        if (search != null && !search.isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        model.addAttribute("users", userPage.getContent());
        addPaginationAttributes(model, page, userPage, search, sortField, sortDir, size); // Updated call
        return "admin/manage-users";
    }

    // ... (Keep create/edit/delete User methods unchanged) ...
    @GetMapping("/users/new")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/create-user";
    }
    @PostMapping("/users/create")
    public String createUser(@RequestParam("fullName") String fullName,
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("confirmPassword") String confirmPassword, // NEW PARAM
                             @RequestParam("role") String role,
                             RedirectAttributes redirectAttributes) {

        // 1. Check if Username exists
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username '" + username + "' already exists.");
            return "redirect:/admin/users/new";
        }

        // 2. NEW: Check if Passwords match
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/admin/users/new";
        }

        // 3. Create User
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

    // --- QUIZZES ---
    @GetMapping("/quizzes")
    public String manageQuizzes(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @RequestParam(value = "sortField", defaultValue = "id") String sortField,
                                @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                                Model model) {

        if (size < 1) size = 10;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Quiz> quizPage;
        if (search != null && !search.isEmpty()) {
            quizPage = quizRepository.searchQuizzes(search, pageable);
        } else {
            quizPage = quizRepository.findAllAndFetchTeacher(pageable);
        }

        model.addAttribute("quizzes", quizPage.getContent());
        addPaginationAttributes(model, page, quizPage, search, sortField, sortDir, size);
        return "admin/manage-quizzes";
    }

    // ... (Keep create/edit/delete/update Quiz methods unchanged) ...
    @GetMapping("/quiz/new")
    public String createQuizForm(Model model) {
        List<Classroom> classes = classroomRepository.findAllWithTeachers();
        model.addAttribute("classes", classes);
        return "admin/create-quiz";
    }
    @PostMapping("/quiz/create")
    public String createQuiz(@RequestParam("title") String title,
                             @RequestParam("classId") Long classId,
                             HttpServletRequest request,
                             Authentication authentication) throws IOException {

        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Class ID"));

        User adminUser = userRepository.findByUsername(authentication.getName()).orElseThrow();
        User classTeacher = classroom.getTeacher();

        MultipartHttpServletRequest multipartRequest = (request instanceof MultipartHttpServletRequest) ? (MultipartHttpServletRequest) request : null;

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setSubject(classroom.getName());
        quiz.setTeacher(classTeacher);
        quiz.setCreator(adminUser);
        quiz.setClassroom(classroom);
        quiz.setPublished(false); // Default to closed
        Quiz savedQuiz = quizRepository.save(quiz);

        Map<String, String[]> parameterMap = request.getParameterMap();
        int questionIndex = 0;

        // Loop through questions
        while (parameterMap.containsKey("questions[" + questionIndex + "].text")) {
            Question question = new Question();
            question.setText(parameterMap.get("questions[" + questionIndex + "].text")[0]);

            // Handle Image
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

            // Loop through options
            while (parameterMap.containsKey("questions[" + questionIndex + "].options[" + optionIndex + "].text")) {
                Option option = new Option();
                option.setText(parameterMap.get("questions[" + questionIndex + "].options[" + optionIndex + "].text")[0]);

                // --- CHANGED LOGIC START ---
                // Old: Read "correctOption" (radio) which held a single index
                // New: Check if this specific option has an "isCorrect" flag (checkbox)
                String isCorrectKey = "questions[" + questionIndex + "].options[" + optionIndex + "].isCorrect";

                // If the checkbox is checked, the parameter exists in the map. If unchecked, it's missing.
                boolean isCorrect = parameterMap.containsKey(isCorrectKey);
                option.setCorrect(isCorrect);
                // --- CHANGED LOGIC END ---

                option.setQuestion(savedQuestion);
                options.add(option);
                optionIndex++;
            }
            optionRepository.saveAll(options);
            questionIndex++;
        }

        return "redirect:/admin/quizzes";
    }
    @GetMapping("/quiz/edit/{id}")
    public String editQuizForm(@PathVariable Long id, Model model) {
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id).orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));
        model.addAttribute("quiz", quiz);
        return "admin/edit-quiz";
    }
    @PostMapping("/quiz/update/{id}")
    public String updateQuiz(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Quiz quiz = quizRepository.findByIdAndFetchQuestionsAndOptions(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quiz Id:" + id));

        quiz.setTitle(request.getParameter("quizTitle"));
        quiz.setSubject(request.getParameter("subject"));

        for (Question question : quiz.getQuestions()) {
            // Update Text
            String qTextParam = request.getParameter("question_" + question.getId());
            if (qTextParam != null) question.setText(qTextParam);

            // Update Options
            for (Option option : question.getOptions()) {
                String oTextParam = request.getParameter("option_" + option.getId());
                if (oTextParam != null) option.setText(oTextParam);

                // CHECKBOX CHECK: Look for "correct_option_{id}"
                String isCorrectParam = request.getParameter("correct_option_" + option.getId());
                option.setCorrect(isCorrectParam != null);
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
        redirectAttributes.addFlashAttribute("success", "Quiz deleted successfully.");
        return "redirect:/admin/quizzes";
    }

    // --- CLASSES ---
    @GetMapping("/classes")
    public String manageClasses(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @RequestParam(value = "sortField", defaultValue = "id") String sortField,
                                @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                                Model model) {

        if (size < 1) size = 10;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Classroom> classPage;
        if (search != null && !search.isEmpty()) {
            classPage = classroomRepository.searchClassrooms(search, pageable);
        } else {
            classPage = classroomRepository.findAllAndFetchTeacher(pageable);
        }

        model.addAttribute("classes", classPage.getContent());
        addPaginationAttributes(model, page, classPage, search, sortField, sortDir, size);
        return "admin/manage-classes";
    }


    @GetMapping("/class/new")
    public String createClassForm(Model model) {
        List<User> teachers = userRepository.findByRole(User.Role.ROLE_TEACHER);
        model.addAttribute("classroom", new Classroom());
        model.addAttribute("teachers", teachers);
        return "admin/create-class";
    }
    @PostMapping("/class/create")
    public String createClass(@RequestParam("name") String name, @RequestParam("teacherId") Long teacherId, RedirectAttributes redirectAttributes) {
        User teacher = userRepository.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Invalid Teacher ID"));
        Classroom classroom = new Classroom();
        classroom.setName(name);
        classroom.setTeacher(teacher);
        classroomRepository.save(classroom);
        redirectAttributes.addFlashAttribute("success", "Class created and assigned to " + teacher.getFullName());
        return "redirect:/admin/classes";
    }
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

    // --- HELPER --
    // FIX: Added 'size' parameter to this method and the model
    private void addPaginationAttributes(Model model, int page, Page<?> pageData, String search, String sortField, String sortDir, int size) {
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("size", size); // New attribute for dropdown
    }
}
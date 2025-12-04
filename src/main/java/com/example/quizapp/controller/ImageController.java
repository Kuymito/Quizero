package com.example.quizapp.controller;

import com.example.quizapp.model.Question;
import com.example.quizapp.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ImageController {

    @Autowired
    private QuestionRepository questionRepository;

    // FIX: Changed return type to ResponseEntity<?> to handle both byte[] and empty responses
    @GetMapping("/image/question/{id}")
    public ResponseEntity<?> getQuestionImage(@PathVariable Long id) {
        Optional<Question> questionOpt = questionRepository.findById(id);

        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            byte[] image = question.getImage();

            if (image != null && image.length > 0) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(image);
            }
        }

        return ResponseEntity.notFound().build();
    }
}
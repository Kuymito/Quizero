package com.example.quizapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Serves the custom login page.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Redirects the root URL to the login page.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}
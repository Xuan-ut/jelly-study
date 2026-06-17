package com.jellystudy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) String search) {
        model.addAttribute("activePage", "index");
        if (search != null) {
            model.addAttribute("searchKeyword", search);
        }
        return "index";
    }

    @GetMapping("/ask")
    public String ask(Model model) {
        model.addAttribute("activePage", "ask");
        return "ask";
    }

    @GetMapping("/knowledge-points")
    public String knowledgePoints(Model model) {
        model.addAttribute("activePage", "knowledge");
        return "knowledge-points";
    }

    @GetMapping("/question/{id}")
    public String question(@PathVariable String id, Model model) {
        model.addAttribute("activePage", "question");
        return "question";
    }

    @GetMapping("/question")
    public String questionWithParam(@RequestParam String id, Model model) {
        model.addAttribute("activePage", "question");
        return "question";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("activePage", "stats");
        return "stats";
    }

    @GetMapping("/companion")
    public String companion(Model model) {
        model.addAttribute("activePage", "companion");
        return "companion";
    }

    @GetMapping("/ai")
    public String ai(Model model) {
        model.addAttribute("activePage", "ai");
        return "ai";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("activePage", "history");
        return "history";
    }

    @GetMapping("/knowledge-point-questions")
    public String knowledgePointQuestions(Model model) {
        model.addAttribute("activePage", "knowledge");
        return "knowledge-point-questions";
    }

    @GetMapping("/plans")
    public String plans(Model model) {
        model.addAttribute("activePage", "plans");
        return "plans";
    }

    @GetMapping("/progress")
    public String progress(Model model) {
        model.addAttribute("activePage", "progress");
        return "progress";
    }

    @GetMapping("/my-activities")
    public String myActivities(Model model) {
        model.addAttribute("activePage", "activities");
        return "my-activities";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("activePage", "profile");
        return "profile";
    }
}

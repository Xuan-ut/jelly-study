package com.jellystudy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/plans")
    public String plans() {
        return "plans";
    }

    @GetMapping("/progress")
    public String progress() {
        return "progress";
    }
}

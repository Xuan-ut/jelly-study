package com.jellystudy.controller;

import com.jellystudy.dubbo.UserDubboService;
import com.jellystudy.entity.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.UserDubboService", timeout = 30000, check = false)
    private UserDubboService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.register(body.get("username"), body.get("password"), body.get("nickname"));
            result.put("success", true);
            result.put("user", user);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return userService.login(body.get("username"), body.get("password"));
    }

    @PostMapping("/guest")
    public Map<String, Object> guestLogin() {
        Map<String, Object> result = new HashMap<>();
        try {
            User guest = userService.createGuest();
            result.put("success", true);
            result.put("user", guest);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.updateProfile(id, body.get("nickname"), body.get("password"));
            result.put("success", true);
            result.put("user", user);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

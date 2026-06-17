package com.jellystudy.controller;

import com.jellystudy.dubbo.StudyPlanDubboService;
import com.jellystudy.dubbo.UserDubboService;
import com.jellystudy.entity.StudyPlan;
import com.jellystudy.entity.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.UserDubboService", timeout = 30000, check = false)
    private UserDubboService userService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

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

    @PutMapping("/{id}/info")
    public Map<String, Object> updateUserInfo(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.updateUserInfo(id, body);
            result.put("success", true);
            result.put("user", user);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}/password")
    public Map<String, Object> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            User user = userService.findById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            if (!user.getPassword().equals(oldPassword)) {
                result.put("success", false);
                result.put("message", "原密码错误");
                return result;
            }
            User updated = userService.updateProfile(id, null, newPassword);
            result.put("success", true);
            result.put("user", updated);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/{id}/avatar")
    public Map<String, Object> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择头像文件");
                return result;
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "只能上传图片文件");
                return result;
            }
            if (file.getSize() > 2 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "头像文件不能超过2MB");
                return result;
            }
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "avatar_" + id + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File dest = new File(dir, filename);
            file.transferTo(dest);
            String avatarUrl = "/uploads/" + filename;
            Map<String, String> info = new HashMap<>();
            info.put("avatar", avatarUrl);
            User user = userService.updateUserInfo(id, info);
            result.put("success", true);
            result.put("avatarUrl", avatarUrl);
            result.put("user", user);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "头像上传失败: " + e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}

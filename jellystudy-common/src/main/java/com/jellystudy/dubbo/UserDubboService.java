package com.jellystudy.dubbo;

import com.jellystudy.entity.User;

import java.util.Map;

public interface UserDubboService {
    User register(String username, String password, String nickname);
    Map<String, Object> login(String username, String password);
    User createGuest();
    User findById(Long id);
    User findByUsername(String username);
    User updateProfile(Long id, String nickname, String password);
    User updateUserInfo(Long id, Map<String, String> info);
}

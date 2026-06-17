package com.jellystudy.service;

import com.jellystudy.dubbo.UserDubboService;
import com.jellystudy.entity.JpaUser;
import com.jellystudy.entity.User;
import com.jellystudy.repository.mysql.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.UserDubboService")
public class UserServiceImpl implements UserDubboService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:info:";
    private static final String USER_SESSION_PREFIX = "user:session:";

    @Override
    public User register(String username, String password, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在: " + username);
        }
        JpaUser jpaUser = new JpaUser(username, password, nickname != null ? nickname : username, "USER");
        jpaUser.setGuest(false);
        JpaUser saved = userRepository.save(jpaUser);
        User user = saved.toUser();
        cacheUserInfo(user);
        log.info("用户注册成功: {}", username);
        return user;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        JpaUser jpaUser = userRepository.findByUsername(username).orElse(null);
        if (jpaUser == null || !jpaUser.getPassword().equals(password)) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }
        User user = jpaUser.toUser();
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_SESSION_PREFIX + token, user.getId(), 7, TimeUnit.DAYS);
        cacheUserInfo(user);
        result.put("success", true);
        result.put("token", token);
        result.put("user", user);
        log.info("用户登录成功: {}", username);
        return result;
    }

    @Override
    public User createGuest() {
        String guestName = "guest_" + UUID.randomUUID().toString().substring(0, 8);
        JpaUser jpaUser = new JpaUser(guestName, "", "游客", "GUEST");
        jpaUser.setGuest(true);
        JpaUser saved = userRepository.save(jpaUser);
        User user = saved.toUser();
        cacheUserInfo(user);
        log.info("创建游客用户: {}", guestName);
        return user;
    }

    @Override
    public User findById(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof User) {
            return (User) cached;
        }
        JpaUser jpaUser = userRepository.findById(id).orElse(null);
        if (jpaUser != null) {
            User user = jpaUser.toUser();
            cacheUserInfo(user);
            return user;
        }
        return null;
    }

    @Override
    public User findByUsername(String username) {
        JpaUser jpaUser = userRepository.findByUsername(username).orElse(null);
        return jpaUser != null ? jpaUser.toUser() : null;
    }

    @Override
    public User updateProfile(Long id, String nickname, String password) {
        JpaUser jpaUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (nickname != null && !nickname.isEmpty()) {
            jpaUser.setNickname(nickname);
        }
        if (password != null && !password.isEmpty()) {
            jpaUser.setPassword(password);
        }
        jpaUser.setUpdateTime(new java.util.Date());
        JpaUser saved = userRepository.save(jpaUser);
        User user = saved.toUser();
        cacheUserInfo(user);
        log.info("更新用户信息: id={}", id);
        return user;
    }

    @Override
    public User updateUserInfo(Long id, Map<String, String> info) {
        JpaUser jpaUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (info.containsKey("nickname") && info.get("nickname") != null && !info.get("nickname").isEmpty()) {
            jpaUser.setNickname(info.get("nickname"));
        }
        if (info.containsKey("signature")) {
            jpaUser.setSignature(info.get("signature"));
        }
        if (info.containsKey("email")) {
            jpaUser.setEmail(info.get("email"));
        }
        if (info.containsKey("avatar")) {
            jpaUser.setAvatar(info.get("avatar"));
        }
        jpaUser.setUpdateTime(new java.util.Date());
        JpaUser saved = userRepository.save(jpaUser);
        User user = saved.toUser();
        cacheUserInfo(user);
        log.info("更新用户详细信息: id={}", id);
        return user;
    }

    private void cacheUserInfo(User user) {
        String cacheKey = USER_CACHE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
    }
}

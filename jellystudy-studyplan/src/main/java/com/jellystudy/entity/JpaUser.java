package com.jellystudy.entity;

import javax.persistence.*;

@Entity
@Table(name = "users")
public class JpaUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;
    private String avatar;
    private String signature;
    private String email;
    private String role;

    @Column(name = "is_guest")
    private boolean isGuest = false;

    @Column(name = "create_time")
    private java.util.Date createTime;

    @Column(name = "update_time")
    private java.util.Date updateTime;

    public JpaUser() {}

    public JpaUser(String username, String password, String nickname, String role) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.createTime = new java.util.Date();
        this.updateTime = new java.util.Date();
    }

    public User toUser() {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setSignature(signature);
        user.setEmail(email);
        user.setRole(role);
        user.setGuest(isGuest);
        user.setCreateTime(createTime);
        user.setUpdateTime(updateTime);
        return user;
    }

    public static JpaUser fromUser(User user) {
        JpaUser jpa = new JpaUser();
        jpa.setId(user.getId());
        jpa.setUsername(user.getUsername());
        jpa.setPassword(user.getPassword());
        jpa.setNickname(user.getNickname());
        jpa.setAvatar(user.getAvatar());
        jpa.setSignature(user.getSignature());
        jpa.setEmail(user.getEmail());
        jpa.setRole(user.getRole());
        jpa.setGuest(user.isGuest());
        jpa.setCreateTime(user.getCreateTime());
        jpa.setUpdateTime(user.getUpdateTime());
        return jpa;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) { isGuest = guest; }
    public java.util.Date getCreateTime() { return createTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public java.util.Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.util.Date updateTime) { this.updateTime = updateTime; }
}

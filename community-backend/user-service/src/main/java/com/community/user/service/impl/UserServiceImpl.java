package com.community.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.user.dto.LoginDTO;
import com.community.user.dto.UserRegisterDTO;
import com.community.user.entity.User;
import com.community.user.mapper.UserMapper;
import com.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean register(UserRegisterDTO dto) {
        // 用户名唯一性检查
        Long cnt = this.lambdaQuery().eq(User::getUsername, dto.getUsername()).count();
        if (cnt != null && cnt > 0) return false;

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole() == null || dto.getRole().isBlank() ? "USER" : dto.getRole());
        return this.save(user);
    }

    @Override
    public User login(LoginDTO dto) {
        User db = this.lambdaQuery().eq(User::getUsername, dto.getUsername()).one();
        if (db == null) return null;
        return passwordEncoder.matches(dto.getPassword(), db.getPassword()) ? db : null;
    }
}

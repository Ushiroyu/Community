package com.community.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.util.ApiResponse;
import com.community.common.util.JwtUtil;
import com.community.user.dto.LoginDTO;
import com.community.user.dto.ResetPasswordDTO;
import com.community.user.dto.UserRegisterDTO;
import com.community.user.dto.UserUpdateDTO;
import com.community.user.entity.User;
import com.community.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ApiResponse register(@RequestBody UserRegisterDTO dto) {
        String normalizedPhone = normalizePhone(dto.getPhone());
        if (normalizedPhone == null) return ApiResponse.error("手机号格式不正确");
        dto.setPhone(normalizedPhone);
        boolean ok = userService.register(dto);
        return ok ? ApiResponse.ok("注册成功") : ApiResponse.error("用户名已存在");
    }

    /**
     * 自助重置密码：校验手机号匹配后重置
     */
    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@RequestBody ResetPasswordDTO dto) {
        if (dto == null || dto.getUsername() == null || dto.getUsername().isBlank()) {
            return ApiResponse.error("用户名不能为空");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            return ApiResponse.error("新密码不能为空");
        }
        User u = userService.lambdaQuery().eq(User::getUsername, dto.getUsername().trim()).one();
        if (u == null) return ApiResponse.error("用户不存在");
        if (u.getPhone() == null || u.getPhone().isBlank()) {
            return ApiResponse.error("该账号未绑定手机号，请联系管理员重置");
        }
        String normalizedPhone = normalizePhone(dto.getPhone());
        if (normalizedPhone == null || !Objects.equals(u.getPhone(), normalizedPhone)) {
            return ApiResponse.error("手机号不匹配，无法重置");
        }
        u.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(u);
        return ApiResponse.ok("密码已重置，请使用新密码登录");
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginDTO dto) {
        User db = userService.login(dto);
        if (db == null) {
            return ApiResponse.error("用户名或密码错误");
        }
        String token = JwtUtil.generateToken(db.getId(), db.getRole());
        return ApiResponse.ok("登录成功")
                .data("token", token)
                .data("userId", db.getId())
                .data("role", db.getRole());
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse getById(@PathVariable Long id) {
        User user = userService.getById(id);
        return user != null
                ? ApiResponse.ok().data("user", user)
                : ApiResponse.error("用户不存在");
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        User u = userService.getById(id);
        if (u == null) {
            return ApiResponse.error("用户不存在");
        }
        boolean changed = false;
        if (dto.nickname() != null) {
            String nickname = dto.nickname().trim();
            String normalizedNickname = nickname.isEmpty() ? null : nickname;
            if (!Objects.equals(u.getNickname(), normalizedNickname)) {
                u.setNickname(normalizedNickname);
                changed = true;
            }
        }
        if (dto.phone() != null) {
            String normalizedPhone = normalizePhone(dto.phone());
            if (dto.phone() != null && !dto.phone().isBlank() && normalizedPhone == null) {
                return ApiResponse.error("手机号格式不正确");
            }
            if (!Objects.equals(u.getPhone(), normalizedPhone)) {
                u.setPhone(normalizedPhone);
                changed = true;
            }
        }
        if (changed) {
            userService.updateById(u);
        }
        return ApiResponse.ok("资料已更新").data("user", u);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN','INTERNAL')")
    public ApiResponse setRole(@PathVariable Long id, @RequestParam String role) {
        User u = userService.getById(id);
        if (u == null) {
            return ApiResponse.error("用户不存在");
        }
        u.setRole(role == null ? "" : role.toUpperCase());
        userService.updateById(u);
        return ApiResponse.ok("角色已更新");
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        String newPassword = null;
        if (body != null) {
            Object v = body.getOrDefault("password", body.get("newPassword"));
            if (v != null) {
                newPassword = String.valueOf(v);
            }
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ApiResponse.error("新密码不能为空");
        }
        User u = userService.getById(id);
        if (u == null) {
            return ApiResponse.error("用户不存在");
        }
        u.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(u);
        return ApiResponse.ok("密码已重置");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse list(@RequestParam(defaultValue = "1") long page,
                            @RequestParam(defaultValue = "10") long size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String role) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(User::getUsername, keyword);
        }
        if (role != null && !role.isBlank()) {
            qw.eq(User::getRole, role.toUpperCase());
        }
        Page<User> p = userService.page(Page.of(page, size), qw);
        return ApiResponse.ok()
                .data("list", p.getRecords())
                .data("total", p.getTotal());
    }

    /**
     * 仅保留数字，长度 6-20 合法，否则返回 null
     */
    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        return (digits.length() >= 6 && digits.length() <= 20) ? digits : null;
    }
}

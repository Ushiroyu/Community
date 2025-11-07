package com.community.user.controller;

import com.community.common.util.ApiResponse;
import com.community.common.util.JwtUtil;
import com.community.user.dto.LoginDTO;
import com.community.user.dto.UserRegisterDTO;
import com.community.user.entity.User;
import com.community.user.service.UserService;
import java.util.Objects;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ApiResponse register(@RequestBody UserRegisterDTO dto) {
        boolean ok = userService.register(dto);
        return ok ? ApiResponse.ok("注册成功") : ApiResponse.error("用户名已存在");
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginDTO dto) {
        User db = userService.login(dto);
        if (db == null) return ApiResponse.error("用户名或密码错误");
        String token = JwtUtil.generateToken(db.getId(), db.getRole());
        return ApiResponse.ok("登录成功")
                .data("token", token)
                .data("userId", db.getId())
                .data("role", db.getRole());
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse getById(@PathVariable Long id) {
        var u = userService.getById(id);
        return (u != null) ? ApiResponse.ok().data("user", u) : ApiResponse.error("用户不存�?);
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody com.community.user.dto.UserUpdateDTO dto) {
        var u = userService.getById(id);
        if (u == null) return ApiResponse.error("�û�������");
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
            String phone = dto.phone().trim();
            String normalizedPhone = phone.isEmpty() ? null : phone;
            if (!Objects.equals(u.getPhone(), normalizedPhone)) {
                u.setPhone(normalizedPhone);
                changed = true;
            }
        }
        if (changed) {
            userService.updateById(u);
        }
        return ApiResponse.ok("�����Ѹ���")
                .data("user", u);
    }

    /**
     * 管理员设置用户角色（ADMIN/LEADER/SUPPLIER/USER�?
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN','INTERNAL')")
    public ApiResponse setRole(@PathVariable Long id, @RequestParam String role) {
        var u = userService.getById(id);
        if (u == null) return ApiResponse.error("用户不存�?);
        u.setRole(role == null ? "" : role.toUpperCase());
        userService.updateById(u);
        return ApiResponse.ok("角色已更�?);
    }

    /**
     * 管理员重置用户密�?
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        String newPassword = null;
        if (body != null) {
            Object v = body.getOrDefault("password", body.get("newPassword"));
            if (v != null) newPassword = String.valueOf(v);
        }
        if (newPassword == null || newPassword.isBlank()) return ApiResponse.error("新密码不能为�?);
        var u = userService.getById(id);
        if (u == null) return ApiResponse.error("用户不存�?);
        u.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(u);
        return ApiResponse.ok("密码已重�?);
    }

    /**
     * 管理员分页查询用�?
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse list(@RequestParam(defaultValue = "1") long page,
                            @RequestParam(defaultValue = "10") long size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String role) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(User::getUsername, keyword);
        }
        if (role != null && !role.isBlank()) {
            qw.eq(User::getRole, role.toUpperCase());
        }
        var p = userService.page(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, size), qw);
        return ApiResponse.ok().data("list", p.getRecords()).data("total", p.getTotal());
    }
}

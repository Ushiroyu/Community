package com.community.user.dto;

import lombok.Data;

@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String role; // 可选：不传则在 Service 里默认 USER
}

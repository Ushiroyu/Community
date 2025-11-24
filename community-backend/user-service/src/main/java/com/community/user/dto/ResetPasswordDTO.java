package com.community.user.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String username;
    private String phone;
    private String newPassword;
}

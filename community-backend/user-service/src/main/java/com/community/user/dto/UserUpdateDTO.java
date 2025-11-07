package com.community.user.dto;

import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @Size(max = 50) String nickname,
        @Size(max = 20) String phone
) {
}

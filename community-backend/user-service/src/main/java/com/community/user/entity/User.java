package com.community.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String role; // USER / LEADER / SUPPLIER / ADMIN
    private String nickname;
    private String phone;
}

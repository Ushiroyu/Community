package com.community.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.user.dto.LoginDTO;
import com.community.user.dto.UserRegisterDTO;
import com.community.user.entity.User;

public interface UserService extends IService<User> {

    /**
     * 用户注册：内部完成唯一性校验与密码加密
     *
     * @param dto 注册参数
     * @return true=成功，false=用户名已存在
     */
    boolean register(UserRegisterDTO dto);

    /**
     * 登录校验（BCrypt）
     *
     * @param dto 登录参数
     * @return 通过返回 User，失败返回 null
     */
    User login(LoginDTO dto);
}

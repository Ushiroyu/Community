package com.community.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.user.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {
    List<Address> listByUserId(Long userId);
}

package com.community.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.user.entity.Address;
import com.community.user.mapper.AddressMapper;
import com.community.user.service.AddressService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {
    @Override
    public List<Address> listByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
    }
}

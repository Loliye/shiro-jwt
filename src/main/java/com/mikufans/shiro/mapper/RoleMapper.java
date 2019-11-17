package com.mikufans.shiro.mapper;

import com.mikufans.shiro.model.dto.RoleDto;
import com.mikufans.shiro.model.dto.UserDto;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface RoleMapper extends Mapper<RoleDto>
{
    //根据user查找role
    List<RoleDto> findRoleByUser(UserDto userDto);
}

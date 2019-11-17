package com.mikufans.shiro.mapper;

import com.mikufans.shiro.model.dto.PermissionDto;
import com.mikufans.shiro.model.dto.RoleDto;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;


public interface PermissionMapper extends Mapper<PermissionDto>
{
    List<PermissionDto> findPermissionByRole(RoleDto roleDto);
}

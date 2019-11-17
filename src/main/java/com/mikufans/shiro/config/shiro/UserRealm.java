package com.mikufans.shiro.config.shiro;

import com.mikufans.shiro.config.shiro.jwt.JwtToken;
import com.mikufans.shiro.mapper.PermissionMapper;
import com.mikufans.shiro.mapper.RoleMapper;
import com.mikufans.shiro.mapper.UserMapper;
import com.mikufans.shiro.model.common.Constant;
import com.mikufans.shiro.model.dto.PermissionDto;
import com.mikufans.shiro.model.dto.RoleDto;
import com.mikufans.shiro.model.dto.UserDto;
import com.mikufans.shiro.util.JwtUtil;
import com.mikufans.shiro.util.RedisUtil;
import com.mikufans.shiro.util.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserRealm extends AuthorizingRealm
{
    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    @Autowired
    public UserRealm(UserMapper userMapper, RoleMapper roleMapper, PermissionMapper permissionMapper)
    {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public boolean supports(AuthenticationToken token)
    {
        return token instanceof JwtToken;
    }

    /**
     * 添加用户角色 权限
     *
     * @param principals
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)
    {
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();

        String account = JwtUtil.getClaim(principals.getPrimaryPrincipal().toString(), Constant.ACCOUNT);

        UserDto userDto = new UserDto();
        userDto.setAccount(account);
        List<RoleDto> roleDtoList = roleMapper.findRoleByUser(userDto);
        for (RoleDto roleDto : roleDtoList)
        {
            simpleAuthorizationInfo.addRole(roleDto.getName());
            List<PermissionDto> permissionDtos = permissionMapper.findPermissionByRole(roleDto);
            for (PermissionDto permissionDto : permissionDtos)
                if (permissionDto != null)
                {
                    simpleAuthorizationInfo.addStringPermission(permissionDto.getPerCode());
                    log.debug("用户加载权限：{}",permissionDto);
                }
        }

        return simpleAuthorizationInfo;
    }


    /**
     * 验证用户名是否正确
     *
     * @param token
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException
    {
        String strToken = (String) token.getCredentials();

        String account = JwtUtil.getClaim(strToken, Constant.ACCOUNT);

        if (StringUtil.isBlank(account))
            throw new AuthenticationException("token账号为空");

        //查找用户是否存在
        UserDto userDto = new UserDto();
        userDto.setAccount(account);
        userDto = userMapper.selectOne(userDto);
        if (userDto == null)
            throw new AuthenticationException("改账号不存在");


        if (JwtUtil.verify(strToken) && RedisUtil.exists(Constant.PREFIX_SHIRO_REFRESH_TOKEN + account))
        {
            String currentTimeRedis = RedisUtil.getObject(Constant.PREFIX_SHIRO_REFRESH_TOKEN + account).toString();
            if (JwtUtil.getClaim(strToken, Constant.CURRENT_TIME_MILLIS).equals(currentTimeRedis))
                return new SimpleAuthenticationInfo(strToken, strToken, "userRealm");

        }
        throw new AuthenticationException("token已过期");

    }
}

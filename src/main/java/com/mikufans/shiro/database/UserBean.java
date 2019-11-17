package com.mikufans.shiro.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBean
{
    private String username;
    private String password;
    private String role;
    private String permission;
}

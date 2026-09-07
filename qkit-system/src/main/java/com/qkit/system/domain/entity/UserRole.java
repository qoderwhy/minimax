package com.qkit.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@TableName("sys_user_role")
public class UserRole {
    private Long userId;
    private Long roleId;
}

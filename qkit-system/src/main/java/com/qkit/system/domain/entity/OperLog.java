package com.qkit.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@TableName("sys_oper_log")
public class OperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;
    private String name;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParam;
    private String responseResult;
    private Integer status;
    private String errorMsg;
    private Long costMs;
    private LocalDateTime operTime;
    private Long createBy;
    private LocalDateTime createTime;
}

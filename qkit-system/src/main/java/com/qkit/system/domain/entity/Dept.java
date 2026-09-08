package com.qkit.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qkit.common.entity.BaseEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@TableName("sys_dept")
public class Dept extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Long parentId;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;

    @TableLogic
    private Integer delFlag;
}

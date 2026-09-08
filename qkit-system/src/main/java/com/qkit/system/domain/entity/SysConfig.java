package com.qkit.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.qkit.common.entity.BaseEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 系统参数配置实体，对应表 sys_config。
 * <p>配置表采用物理删除，无逻辑删除字段 del_flag。</p>
 */
@Getter
@Setter
@RequiredArgsConstructor
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 参数名称 */
    private String configName;

    /** 参数键名（程序取值的 Key） */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 是否系统内置: Y是 N否（内置不可删） */
    private String configType;

    private String remark;
}
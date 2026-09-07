package com.qkit.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 实体基类，提供公共字段。所有业务 Entity 必须继承。
 *
 * <p>公共字段：</p>
 * <ul>
 *     <li>createBy / createTime — 创建人/时间（自动填充）</li>
 *     <li>updateBy / updateTime — 更新人/时间（自动填充）</li>
 *     <li>delFlag — 逻辑删除标志（子类需添加 {@code @TableLogic}）</li>
 * </ul>
 */
@Getter
@Setter
public class BaseEntity {

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

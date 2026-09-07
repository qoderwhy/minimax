package com.qkit.system.enums;

import lombok.Getter;

/**
 * 数据权限范围枚举（5 级）。
 */
@Getter
public enum DataScopeEnum {

    ALL(1, "全部"),
    DEPT_AND_CHILD(2, "本部门及下级"),
    DEPT(3, "本部门"),
    SELF(4, "仅本人"),
    CUSTOM(5, "自定义");

    private final Integer code;
    private final String label;

    DataScopeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DataScopeEnum of(Integer code) {
        if (code == null) return ALL;
        for (DataScopeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return ALL;
    }
}

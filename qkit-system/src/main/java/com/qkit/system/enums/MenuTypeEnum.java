package com.qkit.system.enums;

/**
 * 菜单类型。M=目录、C=菜单、F=按钮。
 */
public enum MenuTypeEnum {
    DIR("M"),
    MENU("C"),
    BUTTON("F");

    private final String code;

    MenuTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MenuTypeEnum of(String code) {
        if (code == null) return null;
        for (MenuTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}

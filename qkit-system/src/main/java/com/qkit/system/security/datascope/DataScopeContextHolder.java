package com.qkit.system.security.datascope;

/**
 * 数据权限上下文 ThreadLocal 存取器。
 */
public final class DataScopeContextHolder {

    private DataScopeContextHolder() {}

    private static final ThreadLocal<DataScopeContext> HOLDER = new ThreadLocal<>();

    public static void set(DataScopeContext ctx) {
        HOLDER.set(ctx);
    }

    public static DataScopeContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

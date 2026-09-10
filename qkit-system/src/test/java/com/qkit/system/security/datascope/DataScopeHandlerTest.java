package com.qkit.system.security.datascope;

import com.qkit.system.enums.DataScopeEnum;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限 SQL 片段拼装单元测试。
 *
 * <p>这是数据隔离的最后一道防线，重点覆盖：范围判定、表名匹配、空集合降级与列名限定。</p>
 */
@DisplayName("数据权限 SQL 片段拼装")
class DataScopeHandlerTest {

    private static final String STMT = "com.qkit.system.mapper.UserMapper.selectPage";

    private final DataScopeHandler handler = new DataScopeHandler();

    @AfterEach
    void clearContext() {
        DataScopeContextHolder.clear();
    }

    @Test
    @DisplayName("无上下文时不追加任何条件")
    void noContext() {
        assertThat(handler.getSqlSegment(new Table("sys_user"), null, STMT)).isNull();
    }

    @Test
    @DisplayName("全部数据范围不追加条件")
    void allScope() {
        DataScopeContextHolder.set(context(DataScopeEnum.ALL, "sys_user", "dept_id", "create_by"));

        assertThat(handler.getSqlSegment(new Table("sys_user"), null, STMT)).isNull();
    }

    @Test
    @DisplayName("表名不匹配时不追加条件")
    void tableMismatchDoesNotFilter() {
        DataScopeContextHolder.set(context(DataScopeEnum.DEPT, "sys_user", "dept_id", null));

        assertThat(handler.getSqlSegment(new Table("sys_role"), null, STMT)).isNull();
    }

    @Test
    @DisplayName("本部门及下级：按部门列 IN 过滤")
    void deptScopeFiltersByDeptColumn() {
        DataScopeContext ctx = context(DataScopeEnum.DEPT_AND_CHILD, "sys_user", "dept_id", null);
        ctx.setVisibleDeptIds(List.of(1L, 2L));
        DataScopeContextHolder.set(ctx);

        Expression segment = handler.getSqlSegment(new Table("sys_user"), null, STMT);

        assertThat(segment).isNotNull();
        assertThat(segment.toString()).isEqualTo("sys_user.dept_id IN (1, 2)");
    }

    @Test
    @DisplayName("仅本人：按用户列等值过滤")
    void selfScopeFiltersByUserColumn() {
        DataScopeContext ctx = context(DataScopeEnum.SELF, "sys_user", "dept_id", "create_by");
        ctx.setUserId(100L);
        DataScopeContextHolder.set(ctx);

        assertThat(handler.getSqlSegment(new Table("sys_user"), null, STMT).toString())
                .isEqualTo("sys_user.create_by = 100");
    }

    @Test
    @DisplayName("可见集合为空时降级为恒假条件，避免越权读到数据")
    void emptyVisibleIdsFallsBackToImpossible() {
        DataScopeContext ctx = context(DataScopeEnum.DEPT, "sys_user", "dept_id", null);
        ctx.setVisibleDeptIds(List.of());
        DataScopeContextHolder.set(ctx);

        assertThat(handler.getSqlSegment(new Table("sys_user"), null, STMT).toString()).isEqualTo("-1 = 1");
    }

    @Test
    @DisplayName("存在表别名时使用别名限定列名")
    void aliasQualifiedColumn() {
        DataScopeContext ctx = context(DataScopeEnum.DEPT, "sys_user", "dept_id", null);
        ctx.setVisibleDeptIds(List.of(3L));
        DataScopeContextHolder.set(ctx);

        Table table = new Table("sys_user");
        table.setAlias(new Alias("u"));

        assertThat(handler.getSqlSegment(table, null, STMT).toString()).isEqualTo("u.dept_id IN (3)");
    }

    private DataScopeContext context(DataScopeEnum scope, String table, String deptColumn, String userColumn) {
        DataScopeContext ctx = new DataScopeContext();
        ctx.setScope(scope);
        ctx.setTable(table);
        ctx.setDeptColumn(deptColumn);
        ctx.setUserColumn(userColumn);
        return ctx;
    }
}

package com.qkit.system.security.datascope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.qkit.system.enums.DataScopeEnum;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MyBatis-Plus 数据权限处理器：按表名匹配上下文，自动追加 dept_id / user_id 过滤条件。
 */
@Component
public class DataScopeHandler implements MultiDataPermissionHandler {

    /** 永假常量：dept_id = -1 或 user_id = -1，用于空集合降级 */
    private static final Expression IMPOSSIBLE = new EqualsTo(new LongValue(-1), new LongValue(1));

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScopeContext ctx = DataScopeContextHolder.get();
        if (ctx == null || ctx.getScope() == DataScopeEnum.ALL) {
            return null;
        }
        if (ctx.getTable() == null || ctx.getTable().isBlank()) {
            return null;
        }
        // 按表名匹配（不区分大小写）。JSqlParser 中 Table.getName() 恒为原始表名，
        // 别名仅作为列限定符使用（见 resolveColumn），因此直接按原始表名匹配即可。
        String targetTable = ctx.getTable().toLowerCase();
        if (!targetTable.equals(table.getName().toLowerCase())) {
            return null;
        }

        DataScopeEnum scope = ctx.getScope();
        Long userId = ctx.getUserId();

        // SELF：用 userColumn 构建 user_id = userId
        if (scope == DataScopeEnum.SELF) {
            if (ctx.getUserColumn() != null && !ctx.getUserColumn().isBlank()) {
                Column col = resolveColumn(table, ctx.getUserColumn());
                return new EqualsTo(col, new LongValue(userId));
            }
            // 兜底：用 deptColumn
            if (ctx.getDeptColumn() != null && !ctx.getDeptColumn().isBlank()) {
                List<Long> deptIds = ctx.getVisibleDeptIds();
                if (deptIds == null || deptIds.isEmpty()) {
                    return IMPOSSIBLE;
                }
                Column col = resolveColumn(table, ctx.getDeptColumn());
                return new InExpression(col, buildInList(deptIds));
            }
            return null;
        }

        // DEPT / DEPT_AND_CHILD / CUSTOM：优先用 deptColumn + visibleDeptIds
        if (ctx.getDeptColumn() != null && !ctx.getDeptColumn().isBlank()) {
            List<Long> deptIds = ctx.getVisibleDeptIds();
            if (deptIds == null || deptIds.isEmpty()) {
                return IMPOSSIBLE;
            }
            Column col = resolveColumn(table, ctx.getDeptColumn());
            return new InExpression(col, buildInList(deptIds));
        }

        // 回退：用 userColumn + visibleUserIds
        if (ctx.getUserColumn() != null && !ctx.getUserColumn().isBlank()) {
            List<Long> userIds = ctx.getVisibleUserIds();
            if (userIds == null || userIds.isEmpty()) {
                return IMPOSSIBLE;
            }
            Column col = resolveColumn(table, ctx.getUserColumn());
            return new InExpression(col, buildInList(userIds));
        }

        return null;
    }

    /** 构建带表限定的列名 */
    private Column resolveColumn(Table table, String columnName) {
        // 使用表别名（如有）或原始表名限定列
        Table qualifier = table.getAlias() != null && !table.getAlias().getName().isBlank()
                ? new Table(table.getAlias().getName())
                : new Table(table.getName());
        return new Column(qualifier, columnName);
    }

    /** 构建 IN (...) 表达式列表 */
    private ParenthesedExpressionList<Expression> buildInList(List<Long> ids) {
        List<Expression> values = ids.stream()
                .map(id -> (Expression) new LongValue(id))
                .collect(Collectors.toList());
        return new ParenthesedExpressionList<>(new ExpressionList<>(values));
    }
}

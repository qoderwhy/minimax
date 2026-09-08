package com.qkit.common.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务工具：提供事务提交后回调能力。
 *
 * <p>用于在事务提交成功后执行缓存刷新、消息发送等动作，
 * 避免事务回滚时对外部状态（如 Redis）造成污染。</p>
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在当前事务提交后执行；若无活动事务则立即执行。
     *
     * @param runnable 提交后需要执行的动作
     */
    public static void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
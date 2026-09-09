package com.qkit.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.framework.security.annotation.DataScope;
import com.qkit.system.domain.dto.OperLogQueryDTO;
import com.qkit.system.domain.entity.OperLog;
import com.qkit.system.domain.vo.OperLogVO;
import com.qkit.system.mapper.OperLogMapper;
import com.qkit.system.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final OperLogMapper operLogMapper;

    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_oper_log", userColumn = "user_id")
    public R<List<OperLogVO>> page(OperLogQueryDTO query) {
        Page<OperLog> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<OperLog>()
                .like(StrUtil.isNotBlank(query.module()), OperLog::getModule, query.module())
                .like(StrUtil.isNotBlank(query.username()), OperLog::getUsername, query.username())
                .eq(query.status() != null, OperLog::getStatus, query.status())
                .orderByDesc(OperLog::getOperTime);
        Page<OperLog> result = operLogMapper.selectPage(page, wrapper);
        return R.ok(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Async
    public void asyncSave(OperLog entity) {
        operLogMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST);
        operLogMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> clean() {
        operLogMapper.delete(new LambdaQueryWrapper<OperLog>().isNotNull(OperLog::getId));
        return R.ok(true);
    }

    private OperLogVO toVO(OperLog e) {
        return new OperLogVO(e.getId(), e.getModule(), e.getName(), e.getUserId(), e.getUsername(),
                e.getIp(), e.getUserAgent(), e.getMethod(), e.getRequestUrl(), e.getRequestMethod(),
                e.getRequestParam(), e.getResponseResult(), e.getStatus(), e.getErrorMsg(),
                e.getCostMs(), e.getOperTime());
    }
}

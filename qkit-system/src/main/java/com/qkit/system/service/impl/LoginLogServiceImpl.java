package com.qkit.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.framework.security.annotation.DataScope;
import com.qkit.system.domain.dto.LoginLogQueryDTO;
import com.qkit.system.domain.entity.LoginLog;
import com.qkit.system.domain.vo.LoginLogVO;
import com.qkit.system.mapper.LoginLogMapper;
import com.qkit.system.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_login_log", userColumn = "user_id")
    public R<List<LoginLogVO>> page(LoginLogQueryDTO query) {
        Page<LoginLog> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StrUtil.isNotBlank(query.username()), LoginLog::getUsername, query.username())
                .eq(query.status() != null, LoginLog::getStatus, query.status())
                .orderByDesc(LoginLog::getLoginTime);
        Page<LoginLog> result = loginLogMapper.selectPage(page, wrapper);
        return R.ok(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST);
        loginLogMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> clean() {
        loginLogMapper.delete(new LambdaQueryWrapper<LoginLog>().isNotNull(LoginLog::getId));
        return R.ok(true);
    }

    private LoginLogVO toVO(LoginLog e) {
        return new LoginLogVO(e.getId(), e.getUserId(), e.getUsername(),
                e.getIp(), e.getUserAgent(), e.getStatus(), e.getMessage(), e.getLoginTime());
    }
}

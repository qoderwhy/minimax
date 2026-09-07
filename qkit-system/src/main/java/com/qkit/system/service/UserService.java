package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.UserVO;

import java.util.List;

public interface UserService {

    R<List<UserVO>> page(UserQueryDTO query);

    R<UserVO> detail(Long id);

    R<Long> create(UserSaveDTO dto);

    R<Boolean> update(UserSaveDTO dto);

    R<Boolean> delete(List<Long> ids);

    R<Boolean> resetPassword(Long userId, String newPassword);

    R<Boolean> assignRole(Long userId, List<Long> roleIds);

    R<Boolean> changePassword(Long userId, PasswordDTO dto);

    /** 根据用户名查询（登录用） */
    com.qkit.system.domain.entity.User getByUsername(String username);

    /** 更新登录信息 */
    void updateLoginInfo(Long userId, String ip);
}

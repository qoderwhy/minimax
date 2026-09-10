package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserProfileUpdateDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface UserService {

    R<List<UserVO>> page(UserQueryDTO query);

    /** 按查询条件导出用户列表（EasyExcel 写入响应流） */
    void export(UserQueryDTO query, HttpServletResponse response);

    R<UserVO> detail(Long id);

    /** 管理端详情：受数据权限约束，超出可见范围按「不存在」处理 */
    R<UserVO> detailInScope(Long id);

    R<Long> create(UserSaveDTO dto);

    R<Boolean> update(UserSaveDTO dto);

    R<Boolean> delete(List<Long> ids);

    R<Boolean> resetPassword(Long userId, String newPassword);

    R<Boolean> assignRole(Long userId, List<Long> roleIds);

    R<Boolean> changePassword(Long userId, PasswordDTO dto);

    /** 当前登录用户完整资料（含部门、岗位、角色） */
    R<UserVO> profile(Long userId);

    /** 更新当前登录用户资料 */
    R<Boolean> updateProfile(Long userId, UserProfileUpdateDTO dto);

    /** 根据用户名查询（登录用） */
    com.qkit.system.domain.entity.User getByUsername(String username);

    /** 根据主键查询 */
    com.qkit.system.domain.entity.User getById(Long id);

    /** 更新登录信息 */
    void updateLoginInfo(Long userId, String ip);
}

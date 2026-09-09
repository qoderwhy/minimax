package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import com.qkit.common.api.R;
import com.qkit.common.util.WebUtil;
import com.qkit.system.domain.dto.LoginDTO;
import com.qkit.system.domain.vo.CaptchaVO;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.LoginVO;
import com.qkit.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "生成图形验证码")
    @SaIgnore
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return authService.captcha();
    }

    @Operation(summary = "登录")
    @SaIgnore
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        return authService.login(dto, WebUtil.getClientIp(request));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        return authService.logout(WebUtil.getClientIp(request));
    }

    @Operation(summary = "当前登录用户信息")
    @GetMapping("/me")
    public R<LoginUserVO> me() {
        return authService.me();
    }

    @Operation(summary = "当前用户权限码列表")
    @GetMapping("/perms")
    public R<java.util.List<String>> perms() {
        R<LoginUserVO> me = authService.me();
        return R.ok(me.getData() == null ? java.util.List.of() : me.getData().permissions());
    }
}

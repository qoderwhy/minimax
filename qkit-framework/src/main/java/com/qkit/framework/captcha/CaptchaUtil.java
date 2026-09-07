package com.qkit.framework.captcha;

import cn.hutool.core.util.RandomUtil;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;

import java.util.UUID;

/**
 * 图形验证码工具。基于 easy-captcha 生成 4 位字符验证码，返回 base64 与 uuid。
 */
public final class CaptchaUtil {

    private CaptchaUtil() {
    }

    /**
     * 生成验证码。
     *
     * @return CaptchaResult { uuid, base64, code }
     */
    public static CaptchaResult generate() {
        SpecCaptcha captcha = new SpecCaptcha(120, 40, 4);
        captcha.setCharType(Captcha.TYPE_DEFAULT);
        String code = captcha.text().toLowerCase();
        String uuid = UUID.randomUUID().toString();
        String base64 = captcha.toBase64();
        return new CaptchaResult(uuid, base64, code);
    }

    public record CaptchaResult(String uuid, String base64, String code) {
    }
}

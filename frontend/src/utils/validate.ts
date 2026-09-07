/**
 * 表单校验规则工具。
 * 与后端校验规则尽量对齐（手机号、邮箱、必填等）。
 */

/** 手机号正则（与后端规则一致） */
export const MOBILE_REGEX = /^$|^1[3-9]\d{9}$/

/** 通用必填规则 */
export function required(message = '该项不能为空') {
  return { required: true, message, trigger: ['blur', 'change'] as const }
}

/** 手机号校验 */
export function mobile(message = '手机号格式不正确') {
  return {
    pattern: MOBILE_REGEX,
    message,
    trigger: 'blur' as const
  }
}

/** 邮箱校验 */
export function email(message = '邮箱格式不正确') {
  return {
    type: 'email' as const,
    message,
    trigger: 'blur' as const
  }
}
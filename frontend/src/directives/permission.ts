import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

/**
 * 按钮级权限指令：v-permission="'system:user:create'"
 * 数组语义为任一满足；超级管理员通配 *:*:* 拥有全部权限。
 */
const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    applyPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    applyPermission(el, binding)
  }
}

function applyPermission(el: HTMLElement, binding: DirectiveBinding) {
  const value = binding.value
  if (value === undefined || value === null) return
  const allow = usePermissionStore().hasPermission(value)
  // 无权限时移除节点；已移除的节点 parentNode 为空，重复更新时自然跳过
  if (!allow && el.parentNode) {
    el.parentNode.removeChild(el)
  }
}

export default permissionDirective
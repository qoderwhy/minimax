import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

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
  if (!value) return
  const perms = usePermissionStore().buttons
  const allow = Array.isArray(value)
    ? value.some((v) => perms.includes(v))
    : perms.includes(value)
  if (!allow) {
    el.parentNode?.removeChild(el)
  }
}

export default permissionDirective

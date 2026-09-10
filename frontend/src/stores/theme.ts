import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/** 可选的配色主题（与 styles/themes.scss 中的 $themes 一一对应） */
export type ThemeColor = 'blue' | 'indigo' | 'emerald' | 'cyan' | 'violet' | 'rose' | 'slate'

/** 组件密度 */
export type ThemeSize = 'small' | 'default' | 'large'

export interface ThemeColorOption {
  value: ThemeColor
  label: string
  /** 预览色，需与 themes.scss 中该主题的 primary 保持一致 */
  preview: string
}

/** 切换面板展示用；新增主题时同步维护此处与 themes.scss */
export const THEME_COLORS: ThemeColorOption[] = [
  { value: 'blue', label: '经典蓝', preview: '#409eff' },
  { value: 'indigo', label: '靛青紫', preview: '#4f46e5' },
  { value: 'emerald', label: '翡翠绿', preview: '#10b981' },
  { value: 'cyan', label: '青碧', preview: '#0ea5e9' },
  { value: 'violet', label: '紫罗兰', preview: '#8b5cf6' },
  { value: 'rose', label: '玫红', preview: '#f43f5e' },
  { value: 'slate', label: '石墨', preview: '#64748b' }
]

export const THEME_SIZES: { value: ThemeSize; label: string }[] = [
  { value: 'small', label: '紧凑' },
  { value: 'default', label: '默认' },
  { value: 'large', label: '宽松' }
]

const STORAGE_KEY = 'qkit-theme'

interface PersistedTheme {
  color: ThemeColor
  dark: boolean
  size: ThemeSize
}

/** 读取本地存储；解析失败时回落到默认值，避免脏数据导致白屏 */
function readPersisted(): Partial<PersistedTheme> {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') as Partial<PersistedTheme>
  } catch {
    return {}
  }
}

/**
 * 主题状态：配色 + 明暗 + 组件密度。
 *
 * 配色与明暗通过 documentElement 上的 `data-theme` 属性和 `dark` 类驱动，
 * 由 styles/themes.scss 与 Element Plus 的暗色变量文件共同消费；
 * 组件密度交由 el-config-provider 的 size 属性生效。
 */
export const useThemeStore = defineStore('theme', () => {
  const persisted = readPersisted()

  const color = ref<ThemeColor>(persisted.color ?? 'blue')
  const dark = ref<boolean>(persisted.dark ?? false)
  const size = ref<ThemeSize>(persisted.size ?? 'default')

  const colorLabel = computed(
    () => THEME_COLORS.find((item) => item.value === color.value)?.label ?? ''
  )
  const sizeLabel = computed(
    () => THEME_SIZES.find((item) => item.value === size.value)?.label ?? ''
  )

  /** 将当前主题写入 DOM 与本地存储（幂等，可在应用启动时重复调用） */
  function apply() {
    const root = document.documentElement
    root.setAttribute('data-theme', color.value)
    root.classList.toggle('dark', dark.value)

    try {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ color: color.value, dark: dark.value, size: size.value })
      )
    } catch {
      // 隐私模式等场景下写入可能失败，不影响主题本身生效
    }
  }

  function setColor(value: ThemeColor) {
    color.value = value
    apply()
  }

  function setDark(value: boolean) {
    dark.value = value
    apply()
  }

  function toggleDark() {
    setDark(!dark.value)
  }

  function setSize(value: ThemeSize) {
    size.value = value
    apply()
  }

  /** 恢复默认主题 */
  function reset() {
    color.value = 'blue'
    dark.value = false
    size.value = 'default'
    apply()
  }

  return {
    color,
    dark,
    size,
    colorLabel,
    sizeLabel,
    apply,
    setColor,
    setDark,
    toggleDark,
    setSize,
    reset
  }
})

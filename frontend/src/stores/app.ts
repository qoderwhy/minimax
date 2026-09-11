import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export interface TagView {
  path: string
  name: string
  title: string
  affix?: boolean
}

/** 标签页持久化 key */
const TAGS_STORAGE_KEY = 'tags'

/** 从 localStorage 读取标签（响应式） */
function loadPersistedTags(): TagView[] {
  try {
    const raw = localStorage.getItem(TAGS_STORAGE_KEY)
    return raw ? (JSON.parse(raw) as TagView[]) : []
  } catch {
    return []
  }
}

/** 应用状态：侧边栏折叠、打开的标签页 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref(false)

  /** 已打开的标签页（初始化时从 localStorage 恢复） */
  const tags = ref<TagView[]>(loadPersistedTags())

  /** 标签变化时自动持久化 */
  watch(
    tags,
    (val) => {
      localStorage.setItem(TAGS_STORAGE_KEY, JSON.stringify(val))
    },
    { deep: true }
  )

  /** 添加标签（已存在则不重复添加） */
  function addTag(tag: TagView) {
    if (!tags.value.some((t) => t.path === tag.path)) {
      tags.value.push(tag)
    }
  }

  /** 关闭标签 */
  function closeTag(path: string): TagView | undefined {
    const index = tags.value.findIndex((t) => t.path === path)
    if (index === -1) return undefined
    const [removed] = tags.value.splice(index, 1)
    return removed
  }

  /** 关闭除指定标签外的其余标签（保留固定标签） */
  function closeOthersTags(path: string): TagView[] {
    const closed = tags.value.filter((t) => t.path !== path && !t.affix)
    tags.value = tags.value.filter((t) => t.path === path || t.affix)
    return closed
  }

  /** 关闭指定标签左侧的标签（保留固定标签） */
  function closeLeftTags(path: string): TagView[] {
    const index = tags.value.findIndex((t) => t.path === path)
    if (index === -1) return []
    const closed = tags.value.slice(0, index).filter((t) => !t.affix)
    tags.value = tags.value.filter((t, i) => i >= index || t.affix)
    return closed
  }

  /** 关闭指定标签右侧的标签（保留固定标签） */
  function closeRightTags(path: string): TagView[] {
    const index = tags.value.findIndex((t) => t.path === path)
    if (index === -1) return []
    const closed = tags.value.slice(index + 1).filter((t) => !t.affix)
    tags.value = tags.value.filter((t, i) => i <= index || t.affix)
    return closed
  }

  /** 关闭全部标签（保留固定标签） */
  function closeAllTags(): TagView[] {
    const closed = tags.value.filter((t) => !t.affix)
    tags.value = tags.value.filter((t) => t.affix)
    return closed
  }

  /** 重置标签（登出时调用，仅保留固定标签） */
  function resetTags() {
    tags.value = tags.value.filter((t) => t.affix)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /** 直接设置折叠状态（窄屏适配时由布局调用） */
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  return {
    sidebarCollapsed,
    tags,
    addTag,
    closeTag,
    closeOthersTags,
    closeLeftTags,
    closeRightTags,
    closeAllTags,
    resetTags,
    toggleSidebar,
    setSidebarCollapsed
  }
})
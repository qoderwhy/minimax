<script setup lang="ts">
import { inject, onBeforeUnmount, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore, type TagView } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

/** 刷新当前页（由布局提供） */
const reload = inject<() => void>('reload', () => {})

/** 路由变化时添加标签 */
watch(
  () => route.path,
  () => {
    if (route.meta?.hidden) return
    appStore.addTag({
      path: route.path,
      name: String(route.name || route.path),
      title: String(route.meta?.title || ''),
      affix: route.meta?.affix === true
    })
  },
  { immediate: true }
)

/** 当前激活标签的类型 */
function tagType(path: string): 'primary' | 'info' {
  return path === route.path ? 'primary' : 'info'
}

/** 关闭标签：若关闭的是当前页则跳转到最后一个剩余标签 */
function handleClose(path: string) {
  const removed = appStore.closeTag(path)
  if (removed && route.path === path) {
    const last = appStore.tags[appStore.tags.length - 1]
    router.push(last ? last.path : '/')
  }
}

/** 批量关闭后，若当前页被关闭则跳转到最后一个剩余标签 */
function jumpIfCurrentClosed(closed: TagView[]) {
  if (!closed.some((t) => t.path === route.path)) return
  const last = appStore.tags[appStore.tags.length - 1]
  router.push(last ? last.path : '/')
}

// ==================== 右键菜单 ====================
const menu = reactive({
  visible: false,
  x: 0,
  y: 0,
  tag: null as TagView | null
})

function openMenu(e: MouseEvent, tag: TagView) {
  menu.tag = tag
  menu.visible = true
  const menuWidth = 130
  const menuHeight = tag.affix ? 46 : 92
  menu.x = Math.min(e.clientX, window.innerWidth - menuWidth - 8)
  menu.y = Math.min(e.clientY, window.innerHeight - menuHeight - 8)
}

function closeMenu() {
  menu.visible = false
  menu.tag = null
}

function handleRefresh() {
  closeMenu()
  reload()
}

function handleCloseCurrent() {
  const tag = menu.tag
  closeMenu()
  if (tag) handleClose(tag.path)
}

function handleCloseOthers() {
  const tag = menu.tag
  closeMenu()
  if (!tag) return
  const closed = appStore.closeOthersTags(tag.path)
  jumpIfCurrentClosed(closed)
}

function handleCloseLeft() {
  const tag = menu.tag
  closeMenu()
  if (!tag) return
  appStore.closeLeftTags(tag.path)
}

function handleCloseRight() {
  const tag = menu.tag
  closeMenu()
  if (!tag) return
  const closed = appStore.closeRightTags(tag.path)
  jumpIfCurrentClosed(closed)
}

function handleCloseAll() {
  closeMenu()
  const closed = appStore.closeAllTags()
  jumpIfCurrentClosed(closed)
}

function handleGlobalClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.tags-context-menu')) {
    closeMenu()
  }
}

function handleGlobalScroll() {
  closeMenu()
}

onMounted(() => {
  document.addEventListener('click', handleGlobalClick)
  document.addEventListener('scroll', handleGlobalScroll, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleGlobalClick)
  document.removeEventListener('scroll', handleGlobalScroll, true)
})

watch(() => route.path, closeMenu)
</script>

<template>
  <div class="tags-view">
    <el-tag
      v-for="tag in appStore.tags"
      :key="tag.path"
      class="tags-item"
      :type="tagType(tag.path)"
      :effect="tag.path === route.path ? 'dark' : 'plain'"
      :closable="!tag.affix"
      disable-transitions
      @click="router.push(tag.path)"
      @contextmenu.prevent="openMenu($event, tag)"
      @close="handleClose(tag.path)"
    >
      {{ tag.title }}
    </el-tag>

    <teleport to="body">
      <div
        v-show="menu.visible"
        class="tags-context-menu"
        :style="{ left: menu.x + 'px', top: menu.y + 'px' }"
        @click.stop
      >
        <ul>
          <li @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            <span>刷新当前</span>
          </li>
          <li v-if="menu.tag && !menu.tag.affix" @click="handleCloseCurrent">
            <el-icon><Close /></el-icon>
            <span>关闭当前</span>
          </li>
          <li v-else class="tags-menu-disabled">
            <el-icon><Close /></el-icon>
            <span>关闭当前</span>
          </li>
          <li class="tags-menu-divider" />
          <li @click="handleCloseOthers">
            <el-icon><CircleClose /></el-icon>
            <span>关闭其它</span>
          </li>
          <li @click="handleCloseLeft">
            <el-icon><DArrowLeft /></el-icon>
            <span>关闭左侧</span>
          </li>
          <li @click="handleCloseRight">
            <el-icon><DArrowRight /></el-icon>
            <span>关闭右侧</span>
          </li>
          <li @click="handleCloseAll">
            <el-icon><FolderDelete /></el-icon>
            <span>关闭全部</span>
          </li>
        </ul>
      </div>
    </teleport>
  </div>
</template>

<style scoped lang="scss">
.tags-view {
  height: 40px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  overflow-x: auto;
  flex-shrink: 0;

  .tags-item {
    cursor: pointer;
    flex-shrink: 0;
  }
}
</style>

<style lang="scss">
/* 右键菜单（teleport 到 body，需全局样式） */
.tags-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 130px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 6px 0;
  font-size: 13px;

  ul {
    margin: 0;
    padding: 0;
    list-style: none;
  }

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    cursor: pointer;
    color: #303133;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f7fa;
    }
  }

  .tags-menu-disabled {
    color: #c0c4cc;
    cursor: not-allowed;

    &:hover {
      background-color: transparent;
    }
  }

  .tags-menu-divider {
    height: 1px;
    margin: 4px 0;
    padding: 0;
    background-color: #ebeef5;
    cursor: default;
  }
}
</style>
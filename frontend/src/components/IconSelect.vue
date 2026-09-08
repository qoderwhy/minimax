<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Component } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
}>()

const iconMap = ElementPlusIconsVue as unknown as Record<string, Component>
const iconNames = Object.keys(iconMap)

const dialogVisible = ref(false)
const keyword = ref('')

const filteredIcons = computed(() => {
  if (!keyword.value.trim()) return iconNames
  const kw = keyword.value.trim().toLowerCase()
  return iconNames.filter((n) => n.toLowerCase().includes(kw))
})

function open() {
  keyword.value = ''
  dialogVisible.value = true
}

function onSelect(name: string) {
  emit('update:modelValue', name)
  dialogVisible.value = false
}

function onInput(v: string) {
  emit('update:modelValue', v)
}
</script>

<template>
  <div class="icon-select">
    <el-input :model-value="modelValue" placeholder="如：User" clearable @update:model-value="onInput">
      <template #prefix>
        <el-icon v-if="modelValue && iconMap[modelValue]"><component :is="iconMap[modelValue]" /></el-icon>
      </template>
    </el-input>
    <el-button @click="open">选择</el-button>
  </div>

  <el-dialog v-model="dialogVisible" title="选择图标" width="720px">
    <el-input v-model="keyword" placeholder="搜索图标名称，如：setting / user" clearable>
      <template #prefix><el-icon><Search /></el-icon></template>
    </el-input>
    <el-scrollbar max-height="360px" class="icon-panel">
      <button
        v-for="name in filteredIcons"
        :key="name"
        type="button"
        class="icon-cell"
        :class="{ active: modelValue === name }"
        :title="name"
        @click="onSelect(name)"
      >
        <el-icon><component :is="iconMap[name]" /></el-icon>
      </button>
      <el-empty v-if="filteredIcons.length === 0" description="未找到匹配图标" :image-size="60" />
    </el-scrollbar>
  </el-dialog>
</template>

<style scoped lang="scss">
.icon-select {
  display: flex;
  gap: 8px;

  .el-input {
    flex: 1;
  }
}

.icon-panel {
  margin-top: 12px;
  padding: 4px;

  :deep(.el-scrollbar__view) {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(48px, 1fr));
    gap: 6px;
  }
}

.icon-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  font-size: 20px;
  color: var(--el-text-color-primary);
  transition: all 0.2s;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary);
  }

  &.active {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}
</style>
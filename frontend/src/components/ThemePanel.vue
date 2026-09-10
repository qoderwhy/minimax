<script setup lang="ts">
import { THEME_COLORS, THEME_SIZES, useThemeStore, type ThemeSize } from '@/stores/theme'

const visible = defineModel<boolean>({ required: true })

const themeStore = useThemeStore()

/** el-radio-group 的回调参数为宽泛联合类型，这里收窄后交给 store */
function onSizeChange(value: string | number | boolean | undefined) {
  themeStore.setSize(value as ThemeSize)
}
</script>

<template>
  <el-drawer v-model="visible" title="主题设置" size="320px" append-to-body>
    <div class="theme-panel">
      <section class="panel-section">
        <h4 class="panel-title">配色主题</h4>
        <div class="color-list">
          <button
            v-for="item in THEME_COLORS"
            :key="item.value"
            type="button"
            class="color-item"
            :class="{ 'is-active': themeStore.color === item.value }"
            :title="item.label"
            @click="themeStore.setColor(item.value)"
          >
            <span class="dot" :style="{ background: item.preview }" />
            <span class="label">{{ item.label }}</span>
          </button>
        </div>
      </section>

      <section class="panel-section">
        <h4 class="panel-title">外观模式</h4>
        <el-radio-group
          :model-value="themeStore.dark ? 'dark' : 'light'"
          @update:model-value="(value) => themeStore.setDark(value === 'dark')"
        >
          <el-radio-button value="light">浅色</el-radio-button>
          <el-radio-button value="dark">深色</el-radio-button>
        </el-radio-group>
      </section>

      <section class="panel-section">
        <h4 class="panel-title">界面密度</h4>
        <el-radio-group :model-value="themeStore.size" @update:model-value="onSizeChange">
          <el-radio-button v-for="item in THEME_SIZES" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </section>

      <el-button class="reset-btn" @click="themeStore.reset()">恢复默认</el-button>
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
.theme-panel {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-lg);
}

.panel-section {
  .panel-title {
    margin: 0 0 var(--app-space-sm);
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-regular);
  }
}

.color-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--app-space-xs);
}

.color-item {
  display: flex;
  align-items: center;
  gap: var(--app-space-xs);
  padding: 9px 10px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color);
  border-radius: var(--app-radius-md);
  cursor: pointer;
  transition: all var(--app-transition);

  &:hover {
    border-color: var(--el-color-primary);
  }

  &.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary);
  }

  .dot {
    flex-shrink: 0;
    width: 14px;
    height: 14px;
    border-radius: 50%;
  }
}

.reset-btn {
  align-self: flex-start;
}
</style>

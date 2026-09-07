<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const tags = computed(() => {
  const list = router.options.routes
    .filter((r) => r.meta?.title && !r.meta?.hidden)
    .map((r) => ({
      path: r.path,
      title: r.meta?.title as string,
      affix: r.meta?.affix
    }))
  return list
})

function onClose(tag: any) {
  if (tag.affix) return
  // 简化：实际应跳到上一个 tag
}
</script>

<template>
  <div class="tags-view">
    <el-tag
      v-for="t in tags"
      :key="t.path"
      :type="route.path.startsWith(t.path) ? 'primary' : 'info'"
      :closable="!t.affix"
      @click="router.push(t.path)"
      @close="onClose(t)"
    >
      {{ t.title }}
    </el-tag>
  </div>
</template>

<style scoped lang="scss">
.tags-view {
  display: flex;
  gap: 8px;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}
</style>

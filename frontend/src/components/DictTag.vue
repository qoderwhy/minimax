<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import type { DictItem } from '@/api/system/dict'
import { useDictStore } from '@/stores/dict'

const props = defineProps<{
  dictType: string
}>()

const items = ref<DictItem[]>([])
const dictStore = useDictStore()

watchEffect(async () => {
  items.value = await dictStore.loadDict(props.dictType)
})
</script>

<template>
  <span>
    <template v-for="item in items" :key="item.id">
      <el-tag v-if="String(item.value) === String($slots.default?.()[0]?.children)" :type="(item.cssClass as any) || 'primary'">
        {{ item.label }}
      </el-tag>
    </template>
  </span>
</template>

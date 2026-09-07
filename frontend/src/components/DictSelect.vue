<script setup lang="ts">
import { ref, watch } from 'vue'
import type { DictItem } from '@/api/system/dict'
import { useDictStore } from '@/stores/dict'

const props = defineProps<{
  dictType: string
  modelValue?: string | number
  clearable?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | number | undefined): void
}>()

const items = ref<DictItem[]>([])
const dictStore = useDictStore()

async function load() {
  items.value = await dictStore.loadDict(props.dictType)
}

watch(() => props.dictType, load, { immediate: true })

function onChange(v: string | number | undefined) {
  emit('update:modelValue', v)
}
</script>

<template>
  <el-select :model-value="modelValue" :clearable="clearable" placeholder="请选择" @update:model-value="onChange">
    <el-option v-for="item in items" :key="item.id" :label="item.label" :value="item.value" />
  </el-select>
</template>

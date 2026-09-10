import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DictItem } from '@/api/system/dict'
import { listDictItem } from '@/api/system/dict'

export const useDictStore = defineStore('dict', () => {
  const dictMap = ref<Record<string, DictItem[]>>({})

  async function loadDict(type: string) {
    if (dictMap.value[type]) return dictMap.value[type]
    const items = await listDictItem(type)
    dictMap.value[type] = items
    return items
  }

  /** 清空全部字典缓存（登出时调用） */
  function clearDict() {
    dictMap.value = {}
  }

  function getDict(type: string): DictItem[] {
    return dictMap.value[type] || []
  }

  function getLabel(type: string, value: string | number): string {
    const item = (dictMap.value[type] || []).find((d) => String(d.value) === String(value))
    return item ? item.label : String(value)
  }

  return { dictMap, loadDict, clearDict, getDict, getLabel }
})

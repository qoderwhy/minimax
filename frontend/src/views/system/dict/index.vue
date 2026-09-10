<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { deleteDictType, deleteDictItem, pageDictItem, pageDictType, saveDictItem, saveDictType, type DictItem, type DictItemSave, type DictType, type DictTypeSave } from '@/api/system/dict'
import { useCrud } from '@/composables/useCrud'

type DictTypeQuery = {
  pageNum: number
  pageSize: number
  type: string
  name: string
  status?: number
}

type DictItemQuery = {
  pageNum: number
  pageSize: number
  label: string
}

/** 当前选中的字典类型 */
const selectedType = ref<DictType | null>(null)

// ---------- 字典类型（左栏） ----------
const {
  query: typeQuery, list: typeList, total: typeTotal, loading: typeLoading,
  dialogVisible: typeDialogVisible, dialogMode: typeDialogMode, form: typeForm, formRef: typeFormRef,
  fetch: fetchTypes, onSearch: searchTypes, onAdd: onTypeAdd, onEdit: onTypeEdit,
  onSave: onTypeSave, onDelete: removeType
} = useCrud<DictType, DictTypeQuery, DictTypeSave>({
  page: (q) => pageDictType(q),
  save: (data) => saveDictType(data),
  remove: (id) => deleteDictType(Number(id)),
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, type: '', name: '', status: undefined }),
  defaultForm: () => ({ id: undefined, type: '', name: '', status: 1, remark: '' }),
  confirmDelete: (row) => `确认删除字典「${row.name}」？`
})

// ---------- 字典项（右栏） ----------
const {
  query: itemQuery, list: itemList, total: itemTotal, loading: itemLoading,
  dialogVisible: itemDialogVisible, dialogMode: itemDialogMode, form: itemForm, formRef: itemFormRef,
  fetch: fetchItems, onSearch: onItemSearch, onAdd: addItem, onEdit: onItemEdit,
  onSave: onItemSave, onDelete: onItemDelete
} = useCrud<DictItem, DictItemQuery, DictItemSave>({
  // 字典项依赖左侧选中的字典类型；未选中时不发请求
  page: (q) => selectedType.value
    ? pageDictItem({ pageNum: q.pageNum, pageSize: q.pageSize, type: selectedType.value.type, label: q.label })
    : Promise.resolve({ list: [], total: 0 }),
  save: (data) => saveDictItem(data),
  remove: (id) => deleteDictItem(Number(id)),
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, label: '' }),
  defaultForm: () => ({ id: undefined, dictType: '', label: '', value: '', sort: 0, status: 1, cssClass: 'primary', remark: '' }),
  afterAdd: (form) => {
    form.dictType = selectedType.value?.type ?? ''
  },
  immediate: false,
  confirmDelete: (row) => `确认删除字典项「${row.label}」？`
})

/** 左侧查询：同时清空右侧从表 */
function onTypeSearch() {
  selectedType.value = null
  itemList.value = []
  itemTotal.value = 0
  searchTypes()
}

/** 左侧删除：若删除的是当前选中类型，同步清空右侧从表 */
async function onTypeDelete(row: DictType) {
  await removeType(row)
  if (selectedType.value?.id === row.id) {
    selectedType.value = null
    itemList.value = []
    itemTotal.value = 0
  }
}

/** 切换选中的字典类型并加载其字典项 */
function onTypeSelect(row: DictType) {
  selectedType.value = row
  itemQuery.pageNum = 1
  itemQuery.label = ''
  fetchItems()
}

/** 新增字典项前必须先选择字典类型 */
function onItemAdd() {
  if (!selectedType.value) {
    ElMessage.warning('请先在左侧选择字典类型')
    return
  }
  addItem()
}
</script>

<template>
  <div class="page dict-page">
    <div class="dict-left">
      <el-card class="type-card">
        <template #header>
          <div class="card-header">
            <span>字典类型</span>
            <el-button v-permission="'system:dict:create'" type="primary" size="small" @click="onTypeAdd">新增</el-button>
          </div>
        </template>
        <el-form :model="typeQuery" inline class="type-search">
          <el-form-item>
            <el-input v-model="typeQuery.name" placeholder="字典名称" clearable style="width: 130px" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="typeQuery.type" placeholder="字典类型" clearable style="width: 130px" @keyup.enter="onTypeSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onTypeSearch">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table
          v-loading="typeLoading"
          :data="typeList"
          border
          highlight-current-row
          size="small"
          height="100%"
          @current-change="(row) => row && onTypeSelect(row as DictType)"
        >
          <el-table-column prop="name" label="字典名称" min-width="120" />
          <el-table-column prop="type" label="字典类型" min-width="120" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button v-permission="'system:dict:update'" type="primary" link size="small" @click.stop="onTypeEdit(row as DictType)">编辑</el-button>
              <el-button v-permission="'system:dict:delete'" type="danger" link size="small" @click.stop="onTypeDelete(row as DictType)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="typeQuery.pageNum"
          v-model:page-size="typeQuery.pageSize"
          :total="typeTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          small
          style="margin-top: 8px; justify-content: flex-end"
          @current-change="fetchTypes"
        />
      </el-card>
    </div>

    <div class="dict-right">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>字典数据{{ selectedType ? `：${selectedType.name} (${selectedType.type})` : '' }}</span>
            <el-button v-permission="'system:dict:create'" type="primary" size="small" @click="onItemAdd">新增字典项</el-button>
          </div>
        </template>
        <el-form :model="itemQuery" inline class="item-search">
          <el-form-item label="标签">
            <el-input v-model="itemQuery.label" placeholder="标签模糊查询" clearable style="width: 200px" @keyup.enter="onItemSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!selectedType" @click="onItemSearch">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="itemLoading" :data="itemList" border size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="label" label="标签" min-width="120" />
          <el-table-column prop="value" label="值" min-width="120" />
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cssClass" label="样式" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.cssClass || 'primary'" effect="plain">{{ row.cssClass || 'primary' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button v-permission="'system:dict:update'" type="primary" link size="small" @click="onItemEdit(row as DictItem)">编辑</el-button>
              <el-button v-permission="'system:dict:delete'" type="danger" link size="small" @click="onItemDelete(row as DictItem)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="itemQuery.pageNum"
          v-model:page-size="itemQuery.pageSize"
          :total="itemTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          small
          style="margin-top: 8px; justify-content: flex-end"
          @current-change="fetchItems"
          @size-change="fetchItems"
        />
      </el-card>
    </div>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogMode === 'add' ? '新增字典' : '编辑字典'" width="500px">
      <el-form ref="typeFormRef" :model="typeForm" label-width="100px">
        <el-form-item label="字典名称" prop="name"><el-input v-model="typeForm.name" /></el-form-item>
        <el-form-item label="字典类型" prop="type"><el-input v-model="typeForm.type" :disabled="typeDialogMode === 'edit'" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="typeForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onTypeSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典项弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemDialogMode === 'add' ? '新增字典项' : '编辑字典项'" width="500px">
      <el-form ref="itemFormRef" :model="itemForm" label-width="100px">
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="itemForm.dictType" disabled />
        </el-form-item>
        <el-form-item label="标签" prop="label"><el-input v-model="itemForm.label" /></el-form-item>
        <el-form-item label="值" prop="value"><el-input v-model="itemForm.value" /></el-form-item>
        <el-form-item label="样式">
          <el-select v-model="itemForm.cssClass" style="width: 100%">
            <el-option label="primary" value="primary" />
            <el-option label="success" value="success" />
            <el-option label="warning" value="warning" />
            <el-option label="info" value="info" />
            <el-option label="danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="itemForm.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="itemForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onItemSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.dict-page {
  display: flex;
  gap: 12px;
  height: 100%;
  padding: 0;

  .dict-left {
    width: 45%;
    min-width: 420px;
    display: flex;

    .type-card {
      flex: 1;
      display: flex;
      flex-direction: column;

      :deep(.el-card__body) {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      .type-search {
        margin-bottom: 8px;
      }

      :deep(.el-table) {
        flex: 1;
      }
    }
  }

  .dict-right {
    flex: 1;
    min-width: 0;
    display: flex;

    :deep(.el-card) {
      flex: 1;
      display: flex;
      flex-direction: column;

      .el-card__body {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      .el-table {
        flex: 1;
      }
    }
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-search {
  margin-bottom: 8px;
}
</style>
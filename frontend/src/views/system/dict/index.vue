<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { deleteDictType, deleteDictItem, pageDictItem, pageDictType, saveDictItem, saveDictType, type DictItem, type DictType } from '@/api/system/dict'

// ---------- 字典类型（左栏） ----------
const typeQuery = reactive({ pageNum: 1, pageSize: 10, type: '', name: '', status: undefined as number | undefined })
const typeList = ref<DictType[]>([])
const typeTotal = ref(0)
const typeLoading = ref(false)
const typeDialogVisible = ref(false)
const typeDialogMode = ref<'add' | 'edit'>('add')
const typeForm = ref<DictType>({ id: '', type: '', name: '', status: 1, remark: '' })
const typeFormRef = ref()

// ---------- 字典项（右栏） ----------
const itemQuery = reactive({ pageNum: 1, pageSize: 10, label: '' })
const itemList = ref<DictItem[]>([])
const itemTotal = ref(0)
const itemLoading = ref(false)
const itemDialogVisible = ref(false)
const itemDialogMode = ref<'add' | 'edit'>('add')
const itemForm = ref<DictItem>({ id: '', dictType: '', label: '', value: '', sort: 0, status: 1, cssClass: 'primary', remark: '' })
const itemFormRef = ref()

/** 当前选中的字典类型 */
const selectedType = ref<DictType | null>(null)

// ---------- 字典类型操作 ----------
async function fetchTypes() {
  typeLoading.value = true
  try {
    const res = await pageDictType(typeQuery)
    typeList.value = res.list
    typeTotal.value = res.total
  } finally {
    typeLoading.value = false
  }
}

function onTypeSearch() {
  typeQuery.pageNum = 1
  selectedType.value = null
  itemList.value = []
  itemTotal.value = 0
  fetchTypes()
}

function onTypeAdd() {
  typeDialogMode.value = 'add'
  typeForm.value = { id: '', type: '', name: '', status: 1, remark: '' }
  typeDialogVisible.value = true
}

function onTypeEdit(row: DictType) {
  typeDialogMode.value = 'edit'
  typeForm.value = { ...row }
  typeDialogVisible.value = true
}

async function onTypeSave() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveDictType(typeForm.value)
  ElMessage.success('保存成功')
  typeDialogVisible.value = false
  fetchTypes()
}

async function onTypeDelete(row: DictType) {
  await ElMessageBox.confirm(`确认删除字典「${row.name}」？`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success('删除成功')
  if (selectedType.value?.id === row.id) {
    selectedType.value = null
    itemList.value = []
    itemTotal.value = 0
  }
  fetchTypes()
}

/** 切换选中的字典类型并加载其字典项 */
function onTypeSelect(row: DictType) {
  selectedType.value = row
  itemQuery.pageNum = 1
  itemQuery.label = ''
  fetchItems()
}

// ---------- 字典项操作 ----------
async function fetchItems() {
  if (!selectedType.value) return
  itemLoading.value = true
  try {
    const res = await pageDictItem({
      pageNum: itemQuery.pageNum,
      pageSize: itemQuery.pageSize,
      type: selectedType.value.type,
      label: itemQuery.label
    })
    itemList.value = res.list
    itemTotal.value = res.total
  } finally {
    itemLoading.value = false
  }
}

function onItemSearch() {
  itemQuery.pageNum = 1
  fetchItems()
}

function onItemAdd() {
  if (!selectedType.value) {
    ElMessage.warning('请先在左侧选择字典类型')
    return
  }
  itemDialogMode.value = 'add'
  itemForm.value = { id: '', dictType: selectedType.value.type, label: '', value: '', sort: 0, status: 1, cssClass: 'primary', remark: '' }
  itemDialogVisible.value = true
}

function onItemEdit(row: DictItem) {
  itemDialogMode.value = 'edit'
  itemForm.value = { ...row }
  itemDialogVisible.value = true
}

async function onItemSave() {
  const valid = await itemFormRef.value?.validate().catch(() => false)
  if (!valid) return
  await saveDictItem(itemForm.value)
  ElMessage.success('保存成功')
  itemDialogVisible.value = false
  fetchItems()
}

async function onItemDelete(row: DictItem) {
  await ElMessageBox.confirm(`确认删除字典项「${row.label}」？`, '提示', { type: 'warning' })
  await deleteDictItem(row.id)
  ElMessage.success('删除成功')
  fetchItems()
}

onMounted(fetchTypes)
</script>

<template>
  <div class="page dict-page">
    <div class="dict-left">
      <el-card class="type-card">
        <template #header>
          <div class="card-header">
            <span>字典类型</span>
            <el-button v-permission="'system:dict:save'" type="primary" size="small" @click="onTypeAdd">新增</el-button>
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
            <el-button v-permission="'system:dict:save'" type="primary" size="small" @click="onItemAdd">新增字典项</el-button>
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
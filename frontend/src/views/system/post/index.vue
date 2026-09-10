<script setup lang="ts">
import { pagePost, savePost, deletePost, type PostItem, type PostSave } from '@/api/system/post'
import { useCrud } from '@/composables/useCrud'

type PostQuery = {
  pageNum: number
  pageSize: number
  name: string
  code: string
  status?: number
}

const {
  query, list, total, loading,
  dialogVisible, dialogMode, form, formRef,
  fetch, onSearch, onAdd, onEdit, onSave, onDelete
} = useCrud<PostItem, PostQuery, PostSave>({
  page: (q) => pagePost(q),
  save: (data) => savePost(data),
  remove: (id) => deletePost(Number(id)),
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, name: '', code: '', status: undefined }),
  defaultForm: () => ({ id: undefined, name: '', code: '', sort: 0, status: 1, remark: '' }),
  confirmDelete: (row) => `确认删除岗位「${row.name}」？`
})
</script>

<template>
  <div class="page">
    <el-card class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="岗位名称"><el-input v-model="query.name" clearable /></el-form-item>
        <el-form-item label="岗位编码"><el-input v-model="query.code" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-button type="primary" v-permission="'system:post:create'" @click="onAdd">新增岗位</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="岗位名称" />
        <el-table-column prop="code" label="岗位编码" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-permission="'system:post:update'" type="primary" link @click="onEdit(row as PostItem)">编辑</el-button>
            <el-button v-permission="'system:post:delete'" type="danger" link @click="onDelete(row as PostItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="fetch"
        @size-change="fetch"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增岗位' : '编辑岗位'" width="500px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="岗位名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="岗位编码" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

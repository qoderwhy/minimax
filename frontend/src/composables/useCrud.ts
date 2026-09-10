import { nextTick, ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePagination, type PageQuery, type UsePaginationOptions } from './usePagination'

export interface UseCrudOptions<Row, Query extends PageQuery, Form> extends UsePaginationOptions<Row, Query> {
  /** 保存接口：新增与更新共用（接口内部按 id 判断） */
  save: (data: Form) => Promise<unknown>
  /** 删除接口 */
  remove: (id: number | string) => Promise<unknown>
  /** 表单初始值工厂 */
  defaultForm: () => Form
  /** 编辑回显：默认浅拷贝行数据；需要详情时在此异步获取 */
  toForm?: (row: Row) => Form | Promise<Form>
  /** 编辑弹窗内容渲染后的补充动作（如回显授权树勾选） */
  afterEdit?: (row: Row, form: Form) => void | Promise<void>
  /** 新增弹窗打开后的补充动作（如清空授权树勾选） */
  afterAdd?: (form: Form) => void | Promise<void>
  /** 保存前的加工（如从弹窗组件取值组装关联数据） */
  beforeSave?: (form: Form) => void | Promise<void>
  /** 保存成功后的附加动作（如提交角色/菜单授权）；saved 为保存接口的原始返回 */
  afterSave?: (saved: unknown, form: Form) => void | Promise<void>
  /** 删除前置校验：返回 false 表示中止删除（如内置数据不可删） */
  beforeDelete?: (row: Row) => boolean
  /** 删除确认文案：返回 null 表示不弹确认 */
  confirmDelete?: (row: Row) => string | null
  /** 取行主键，默认取 row.id */
  rowId?: (row: Row) => number | string
}

/**
 * 列表页增删改查通用逻辑：在 usePagination 之上叠加弹窗、表单与增删改动作。
 *
 * <p>返回值命名与列表页原有模板绑定保持一致（query/list/total/loading/dialogVisible/
 * dialogMode/form/formRef/fetch/onSearch/onReset/onAdd/onEdit/onSave/onDelete），
 * 因此页面模板无需改动。</p>
 */
export function useCrud<Row, Query extends PageQuery, Form>(options: UseCrudOptions<Row, Query, Form>) {
  const pagination = usePagination<Row, Query>(options)

  const dialogVisible = ref(false)
  const dialogMode = ref<'add' | 'edit'>('add')
  const form = ref(options.defaultForm()) as Ref<Form>
  const formRef = ref()

  function onAdd() {
    dialogMode.value = 'add'
    form.value = options.defaultForm()
    dialogVisible.value = true
    nextTick(() => options.afterAdd?.(form.value))
  }

  async function onEdit(row: Row) {
    dialogMode.value = 'edit'
    form.value = options.toForm ? await options.toForm(row) : ({ ...row } as unknown as Form)
    dialogVisible.value = true
    // 等待弹窗内容渲染完成，再执行依赖组件已挂载的补充动作（如授权树勾选）
    await nextTick()
    await options.afterEdit?.(row, form.value)
  }

  async function onSave() {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    await options.beforeSave?.(form.value)
    const saved = await options.save(form.value)
    await options.afterSave?.(saved, form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    pagination.fetch()
  }

  async function onDelete(row: Row) {
    if (options.beforeDelete && !options.beforeDelete(row)) return
    const message = options.confirmDelete ? options.confirmDelete(row) : null
    if (message) {
      try {
        await ElMessageBox.confirm(message, '提示', { type: 'warning' })
      } catch {
        // 用户取消删除
        return
      }
    }
    const id = options.rowId ? options.rowId(row) : (row as unknown as { id: number }).id
    await options.remove(id)
    ElMessage.success('删除成功')
    pagination.fetch()
  }

  return {
    ...pagination,
    dialogVisible,
    dialogMode,
    form,
    formRef,
    onAdd,
    onEdit,
    onSave,
    onDelete
  }
}

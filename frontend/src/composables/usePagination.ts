import { onMounted, reactive, ref, type Ref } from 'vue'

/** 分页查询返回结构 */
export interface PageResult<Row> {
  list: Row[]
  total: number
}

/** 分页查询条件的最小约定 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface UsePaginationOptions<Row, Query extends PageQuery> {
  /** 分页查询接口 */
  page: (query: Query) => Promise<PageResult<Row>>
  /** 查询条件初始值工厂；重置时重新调用，避免多个实例共享同一对象 */
  defaultQuery: () => Query
  /** 请求前的参数加工（如注入时间范围等不在 query 中的条件） */
  transformQuery?: (query: Query) => Query
  /** 是否在组件挂载时自动加载，默认 true */
  immediate?: boolean
}

/**
 * 分页列表通用逻辑：查询条件、列表、总数、加载态，以及查询/重置动作。
 *
 * <p>只负责「分页查询」，不含弹窗与增删改，供纯列表页直接使用，也作为 useCrud 的基座。</p>
 */
export function usePagination<Row, Query extends PageQuery>(options: UsePaginationOptions<Row, Query>) {
  const query = reactive(options.defaultQuery()) as unknown as Query
  const list = ref([]) as Ref<Row[]>
  const total = ref(0)
  const loading = ref(false)

  async function fetch() {
    loading.value = true
    try {
      const params = options.transformQuery ? options.transformQuery(query) : query
      const res = await options.page(params)
      list.value = res.list ?? []
      total.value = res.total ?? 0
    } finally {
      loading.value = false
    }
  }

  /** 查询：回到第一页 */
  function onSearch() {
    query.pageNum = 1
    fetch()
  }

  /** 重置查询条件后重新查询 */
  function onReset() {
    Object.assign(query, options.defaultQuery())
    fetch()
  }

  if (options.immediate !== false) {
    onMounted(fetch)
  }

  return { query, list, total, loading, fetch, onSearch, onReset }
}

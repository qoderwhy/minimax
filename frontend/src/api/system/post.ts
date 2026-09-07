import request from '@/utils/request'

export interface PostItem {
  id: string
  name: string
  code: string
  sort: number
  status: number
  remark?: string
}

export interface PostSave {
  id?: string
  name: string
  code: string
  sort: number
  status: number
  remark?: string
}

export function pagePost(params: { pageNum?: number; pageSize?: number; name?: string; code?: string; status?: number }) {
  return request.page<PostItem>({ url: '/admin-api/system/post/page', params })
}

export function listPost() {
  return request.get<PostItem[]>({ url: '/admin-api/system/post/list' })
}

export function savePost(data: PostSave) {
  return data.id ? request.put<void>({ url: '/admin-api/system/post', data }) : request.post<void>({ url: '/admin-api/system/post', data })
}

export function deletePost(id: string) {
  return request.delete<void>({ url: `/admin-api/system/post/${id}` })
}

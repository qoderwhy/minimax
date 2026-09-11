import request from '@/utils/request'

export interface PostItem {
  id: number
  name: string
  code: string
  sort: number
  status: number
  remark?: string
}

export interface PostSave {
  id?: number
  name: string
  code: string
  sort: number
  status: number
  remark?: string
}

export function pagePost(params: { pageNum?: number; pageSize?: number; name?: string; code?: string; status?: number }) {
  return request.page<PostItem>({ url: '/system/post/page', params })
}

export function listPost() {
  return request.get<PostItem[]>({ url: '/system/post/list' })
}

export function savePost(data: PostSave) {
  return data.id ? request.put<void>({ url: '/system/post/update', data }) : request.post<void>({ url: '/system/post/create', data })
}

export function deletePost(id: number) {
  return request.delete<void>({ url: '/system/post/delete', data: [id] })
}
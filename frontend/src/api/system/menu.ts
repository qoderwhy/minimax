import request from '@/utils/request'

export interface MenuItem {
  id: string
  parentId: string
  name: string
  type: number
  path?: string
  component?: string
  perm?: string
  icon?: string
  sort: number
  status: number
  visible: number
  keepAlive?: number
  children?: MenuItem[]
}

export interface MenuSave {
  id?: string
  parentId: string
  name: string
  type: number
  path?: string
  component?: string
  perm?: string
  icon?: string
  sort: number
  status: number
  visible: number
  keepAlive?: number
}

export function treeMenu() {
  return request.get<MenuItem[]>({ url: '/admin-api/system/menu/tree' })
}

export function listMenu() {
  return request.get<MenuItem[]>({ url: '/admin-api/system/menu/tree' })
}

export function saveMenu(data: MenuSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/menu/update', data })
    : request.post<void>({ url: '/admin-api/system/menu/create', data })
}

export function deleteMenu(id: string) {
  return request.delete<void>({ url: '/admin-api/system/menu/delete', data: [id] })
}

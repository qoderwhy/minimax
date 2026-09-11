import request from '@/utils/request'

export interface MenuItem {
  id: number
  parentId: number
  name: string
  type: string
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
  id?: number
  parentId: number
  name: string
  type: string
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
  return request.get<MenuItem[]>({ url: '/system/menu/tree' })
}

export function saveMenu(data: MenuSave) {
  return data.id
    ? request.put<void>({ url: '/system/menu/update', data })
    : request.post<void>({ url: '/system/menu/create', data })
}

export function deleteMenu(id: number) {
  return request.delete<void>({ url: '/system/menu/delete', data: [id] })
}
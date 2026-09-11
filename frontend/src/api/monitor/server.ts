import request from '@/utils/request'

export interface CpuInfo {
  cores: number
  userUsage: number
  systemUsage: number
  freeRate: number
}

export interface MemoryInfo {
  totalMemory: number
  usedMemory: number
  freeMemory: number
  usageRate: number
  jvmTotalMemory: number
  jvmUsedMemory: number
  jvmFreeMemory: number
  jvmUsageRate: number
}

export interface ServerDetail {
  name: string
  ip: string
  os: string
  arch: string
}

export interface JvmInfo {
  javaName: string
  javaVersion: string
  startTime: string
  runTime: string
  installPath: string
  projectPath: string
  runArgs: string
}

export interface DiskInfo {
  path: string
  fileSystem: string
  type: string
  totalSize: number
  freeSize: number
  usedSize: number
  usageRate: number
}

export interface ServerInfo {
  cpu: CpuInfo
  memory: MemoryInfo
  server: ServerDetail
  jvm: JvmInfo
  disks: DiskInfo[]
}

export function getServerInfo() {
  return request.get<ServerInfo>({ url: '/monitor/server/page' })
}

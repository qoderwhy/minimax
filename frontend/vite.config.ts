import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 空前缀 ''：读取 .env / .env.* 中的全部变量（含不带 VITE_ 前缀的服务端专用变量）
  const env = loadEnv(mode, process.cwd(), '')

  // 代理目标取环境变量，缺省回落到本地默认端口，避免硬编码。
  const proxyTarget = (key: string, fallback: string): string => env[key] || fallback

  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/auto-imports.d.ts'
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts'
      })
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      // 开发代理：路径前缀 -> 对应后端（目标取 PROXY_* 变量，缺省本地端口）。
      // 新增服务：.env.development 加 PROXY_XXX，下面加 '/xxx' 条目即可。
      // 仅当请求用相对 baseURL 时走代理；当前 admin-api 用绝对地址直连(CORS)，
      // 想走代理则把 .env.development 的 VITE_API_BASE_URL 改为 '/admin-api'。
      proxy: {
        // 主后台 qkit-admin（context-path=/admin-api）
        '/admin-api': {
          target: proxyTarget('PROXY_ADMIN', 'http://localhost:8080'),
          changeOrigin: true
        }
        // 其它微服务示例：'/analytics': { target: proxyTarget('PROXY_ANALYTICS', 'http://localhost:8081'), changeOrigin: true }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          /**
           * 按依赖来源拆包：element-plus 不做强制归组（否则 883KB 整库被 modulepreload 进首屏），
           * 交由 Rollup 按引用关系切分；图标库(约290个)体积固定且多页共享，单独成 chunk 长期缓存。
           */
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('@element-plus/icons-vue')) return 'icons'
            if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
            if (/[\\/]node_modules[\\/](vue|@vue|vue-router|pinia)[\\/]/.test(id)) return 'vue'
            if (/[\\/]node_modules[\\/](axios|dayjs|nprogress)[\\/]/.test(id)) return 'vendor'
          }
        }
      }
    }
  }
})

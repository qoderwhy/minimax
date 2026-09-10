import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
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
      proxy: {
        '/admin-api': {
          target: env.VITE_API_BASE || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          /**
           * 按依赖来源拆分产物。
           *
           * element-plus 刻意不做强制归组：强制归组会让整个组件库成为入口的静态依赖，
           * 883 KB 的组件库因此被 modulepreload 进首屏。交由 Rollup 依据引用关系切分后，
           * 仅布局与登录页用到的组件留在首屏，table / date-picker / tree 等重型组件
           * 随对应路由懒加载，被多个页面共享的组件则由 Rollup 自动提升为公共 chunk。
           *
           * 图标库（约 290 个）体积固定且被多页面共享，单独成 chunk 以便长期缓存。
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

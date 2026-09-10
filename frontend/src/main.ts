import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import permissionDirective from './directives/permission'
import { setupIcons } from './utils/icons'

/**
 * Element Plus 样式按需引入。
 *
 * 组件样式由 unplugin-vue-components 的 ElementPlusResolver 在编译期注入，
 * 这里只补 resolver 覆盖不到的两类：
 * 1. base.css —— 定义全部 --el-* 基础变量，缺少会导致组件样式整体错乱；
 * 2. 函数式组件样式 —— ElMessage / ElMessageBox / ElNotification 由 JS 调用
 *    （各页面手写 import），resolver 只识别模板标签，无法自动补样式。
 * 注：v-loading 指令由 resolver 自动处理，无需手动注册。
 */
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-notification.css'
// 暗色主题变量：由 html.dark 类激活
import 'element-plus/theme-chalk/dark/css-vars.css'

// 项目主题与全局样式，需置于 Element Plus 样式之后以便覆盖其变量
import './styles/index.scss'

const app = createApp(App)

setupIcons(app)

app.use(createPinia())
app.use(router)
app.directive('permission', permissionDirective)

app.mount('#app')

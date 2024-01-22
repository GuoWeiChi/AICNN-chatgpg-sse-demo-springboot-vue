import { createRouter, createWebHistory } from 'vue-router'
import Home from './views/Home.vue'

// ... 导入其他demo组件

const routes = [
    { path: '/', component: Home }
    // ... 可能的其他路由
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
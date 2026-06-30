import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/main.css'

createApp(App).use(createPinia()).use(router).mount('#app')

// 旧版本曾使用 Service Worker 缓存图片；升级到纯代码 UI 后主动清理，避免浏览器继续返回旧页面资源。
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then(registrations => {
    registrations.forEach(registration => registration.unregister())
  })
}
if ('caches' in globalThis) {
  caches.keys().then(keys => Promise.all(keys
    .filter(key => key.startsWith('ai-werewolf-static-'))
    .map(key => caches.delete(key))))
}

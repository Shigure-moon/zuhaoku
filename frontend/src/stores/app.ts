import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const collapsed = ref(false) // 侧边栏折叠状态
  const theme = ref<'light' | 'dark'>('light') // 主题

  const toggleCollapsed = () => {
    collapsed.value = !collapsed.value
  }

  const setTheme = (newTheme: 'light' | 'dark') => {
    theme.value = newTheme
    document.documentElement.classList.toggle('dark', newTheme === 'dark')
  }

  return {
    collapsed,
    theme,
    toggleCollapsed,
    setTheme,
  }
})


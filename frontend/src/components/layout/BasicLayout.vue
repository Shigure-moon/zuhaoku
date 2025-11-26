<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <el-icon class="collapse-icon" @click="appStore.toggleCollapsed">
          <Menu />
        </el-icon>
        <span class="logo">租号酷</span>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl || userStore.userInfo?.avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.mobile || '用户' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside :width="appStore.collapsed ? '64px' : '200px'" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.collapsed"
          router
          class="layout-menu"
        >
          <el-menu-item index="/tenant/account-list" v-if="isTenant">
            <el-icon><List /></el-icon>
            <template #title>账号列表</template>
          </el-menu-item>
          <el-menu-item index="/tenant/orders" v-if="isTenant">
            <el-icon><Document /></el-icon>
            <template #title>我的订单</template>
          </el-menu-item>
          <el-menu-item index="/owner/accounts" v-if="isOwner">
            <el-icon><Box /></el-icon>
            <template #title>我的账号</template>
          </el-menu-item>
          <el-menu-item index="/owner/orders" v-if="isOwner">
            <el-icon><Document /></el-icon>
            <template #title>订单管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/dashboard" v-if="isOperator">
            <el-icon><Monitor /></el-icon>
            <template #title>工作台</template>
          </el-menu-item>
          <el-menu-item index="/admin/appeals" v-if="isOperator">
            <el-icon><Warning /></el-icon>
            <template #title>申诉管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/users" v-if="isOperator">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/audit-logs" v-if="isOperator">
            <el-icon><Document /></el-icon>
            <template #title>日志审计</template>
          </el-menu-item>
          <el-menu-item index="/admin/risk" v-if="isOperator">
            <el-icon><Warning /></el-icon>
            <template #title>风控管理</template>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { ElMessageBox } from 'element-plus'
import { Menu, ArrowDown, List, Document, Box, User, Monitor, Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)

// 角色判断
const isTenant = computed(() => {
  const role = userStore.userInfo?.role?.toUpperCase()
  return role === 'TENANT'
})

const isOwner = computed(() => {
  const role = userStore.userInfo?.role?.toUpperCase()
  return role === 'OWNER'
})

const isOperator = computed(() => {
  const role = userStore.userInfo?.role?.toUpperCase()
  return role === 'OPERATOR'
})

const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      userStore.logout()
      router.push('/login')
    })
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-icon {
      font-size: 20px;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #409eff;
      }
    }

    .logo {
      font-size: 20px;
      font-weight: bold;
      color: #409eff;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background 0.3s;

      &:hover {
        background: #f5f7fa;
      }

      .username {
        font-size: 14px;
      }
    }
  }
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s;

  .layout-menu {
    border-right: none;
    height: 100%;
  }
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>


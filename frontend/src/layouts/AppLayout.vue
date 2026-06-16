<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { Heart, LayoutDashboard, PawPrint, FileText, LogOut } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const nav = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/patients', label: 'Pacientes', icon: PawPrint },
  { to: '/documents', label: 'Documentos', icon: FileText },
]

function handleLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="flex min-h-svh bg-background">
    <aside class="flex w-64 flex-col border-r bg-sidebar text-sidebar-foreground">
      <div class="flex items-center gap-2 px-6 py-5 border-b">
        <Heart class="size-6 text-red-500" />
        <span class="text-lg font-semibold tracking-tight">CardioVet</span>
      </div>

      <nav class="flex-1 space-y-1 p-3">
        <RouterLink
          v-for="item in nav"
          :key="item.to"
          :to="item.to"
          class="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
          active-class="bg-sidebar-accent text-sidebar-accent-foreground"
          exact-active-class="bg-sidebar-accent text-sidebar-accent-foreground"
        >
          <component :is="item.icon" class="size-4" />
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="border-t p-3">
        <div class="px-3 py-2 text-sm">
          <p class="font-medium leading-none">{{ auth.user?.name }}</p>
          <p class="mt-1 text-xs text-muted-foreground">{{ auth.user?.role }}</p>
        </div>
        <Button variant="ghost" class="w-full justify-start gap-3" @click="handleLogout">
          <LogOut class="size-4" />
          Sair
        </Button>
      </div>
    </aside>

    <main class="flex-1 overflow-auto">
      <div class="mx-auto max-w-6xl p-8">
        <RouterView />
      </div>
    </main>
  </div>
</template>

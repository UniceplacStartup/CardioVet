<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { Upload, FileText, Users, UserCircle, LogOut } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import logoFull from '@/assets/logo-full.png'
import perfImg from '@/assets/perf.png'

const auth = useAuthStore()
const router = useRouter()

const nav = [
  { to: '/upload', label: 'UPLOAD', icon: Upload },
  { to: '/laudos', label: 'MEUS LAUDOS', icon: FileText },
  { to: '/pacientes', label: 'PACIENTES', icon: Users },
  { to: '/perfil', label: 'MEU PERFIL', icon: UserCircle },
]

function handleLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="flex h-svh w-full overflow-hidden bg-[#16273e] font-sans">
    <!-- Barra lateral fixa -->
    <aside class="flex h-full w-64 shrink-0 flex-col justify-between bg-[#ECEFF3] border-r border-slate-300/70 p-4 select-none overflow-y-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      <div>
        <!-- Logo oficial no topo da barra -->
        <div class="flex justify-center pb-8 pt-2">
          <img
            :src="logoFull"
            alt="CardioVet — Laudos cardiológicos"
            class="w-48 max-w-full"
          />
        </div>

        <!-- Menu de Navegação -->
        <nav class="space-y-2">
          <RouterLink
            v-for="item in nav"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-3 rounded-lg px-4 py-2.5 text-xs font-semibold tracking-wider transition-all"
            :class="$route.path.startsWith(item.to)
              ? 'bg-[#4F637C] text-white shadow-sm'
              : 'text-[#4A5D75] hover:bg-[#DDE2E8] hover:text-slate-900'"
          >
            <component :is="item.icon" class="size-4 shrink-0" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </div>

      <!-- Rodapé da barra lateral: Perfil e Botão Sair -->
      <div class="space-y-3 pt-6">
        <!-- Widget do perfil do usuário -->
        <div class="flex items-center gap-2.5 rounded-xl border border-slate-300/80 bg-white p-2.5 shadow-xs">
          <img
            :src="perfImg"
            alt="Dra. Aline Rosa"
            class="size-9 rounded-full object-cover shrink-0 ring-1 ring-slate-200"
          />
          <div class="min-w-0 flex-1 text-left">
            <p class="truncate text-[10.5px] font-bold leading-tight text-slate-800">
              Clínica Veterinária José da Silva
            </p>
            <p class="truncate text-[9.5px] font-medium text-slate-500">
              Med. Vet. {{ auth.user?.name || 'Aline Rosa' }}
            </p>
            <div class="flex items-center justify-between text-[8.5px] text-slate-400">
              <span class="truncate">{{ auth.user?.email || 'admin@email.com' }}</span>
              <span class="shrink-0 pl-1 font-medium">CRMV - XXXX</span>
            </div>
          </div>
        </div>

        <!-- Botão SAIR -->
        <button
          type="button"
          class="flex w-full items-center gap-2 px-2 py-1.5 text-xs font-bold tracking-wider text-brand-red uppercase transition-colors hover:opacity-80 cursor-pointer"
          @click="handleLogout"
        >
          <LogOut class="size-4 stroke-[2.5]" />
          <span>SAIR</span>
        </button>
      </div>
    </aside>

    <!-- Área principal do conteúdo -->
    <main class="flex-1 h-full overflow-y-auto bg-[#16273e] p-6 sm:p-8 lg:p-10">
      <div class="mx-auto max-w-7xl">
        <RouterView />
      </div>
    </main>
  </div>
</template>

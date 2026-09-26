<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search, FileText, Loader2 } from 'lucide-vue-next'
import api from '@/lib/api'
import type { Page, Patient } from '@/types'

const patients = ref<Patient[]>([])
const loading = ref(false)
const search = ref('')

async function loadFromApi() {
  loading.value = true
  try {
    const { data } = await api.get<Page<Patient>>('/patients', {
      params: { size: 100 },
    })
    if (data.content && data.content.length > 0) {
      patients.value = data.content
    } else {
      patients.value = []
    }
  } catch {
    patients.value = []
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  if (!search.value.trim()) return patients.value
  const q = search.value.toLowerCase()
  return patients.value.filter(
    (p) =>
      p.name.toLowerCase().includes(q) ||
      (p.breed && p.breed.toLowerCase().includes(q)) ||
      (p.tutorName && p.tutorName.toLowerCase().includes(q)),
  )
})

onMounted(() => {
  loadFromApi()
})
</script>

<template>
  <div class="space-y-4 select-none">
    <!-- Título da página -->
    <h1 class="text-3xl font-bold tracking-tight text-white">
      Pacientes
    </h1>

    <!-- Container principal -->
    <div class="rounded-[18px] bg-[#D9DDE2] p-6 sm:p-8 shadow-xl">
      <!-- Barra de pesquisa com lupa -->
      <div class="relative mb-6">
        <Search class="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-slate-500" />
        <input
          v-model="search"
          type="text"
          placeholder="Pesquisar paciente"
          class="h-12 w-full rounded-xl border border-slate-400/80 bg-transparent pl-12 pr-4 text-sm font-medium text-[#14253B] placeholder:text-slate-500 outline-none transition-all focus:border-[#14253B] focus:bg-white/40 focus:ring-1 focus:ring-[#14253B]"
        />
      </div>

      <!-- Subtítulo -->
      <h2 class="mb-4 text-xl font-bold text-[#14253B]">
        Pacientes cadastrados
      </h2>

      <!-- Lista de cards de pacientes -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-16 text-slate-500">
        <Loader2 class="size-7 animate-spin text-brand-red mb-2" />
        <span class="text-xs font-medium">Carregando pacientes...</span>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="p in filtered"
          :key="p.id"
          class="flex items-center justify-between rounded-xl border border-slate-400/70 bg-[#E5E9EE] px-4 py-3.5 transition-all hover:border-slate-500 hover:bg-[#DFE3E9] hover:shadow-xs"
        >
          <!-- Informações do animal -->
          <div class="flex items-center gap-3.5">
            <div class="flex size-9 items-center justify-center rounded-lg bg-slate-300/60 text-slate-700">
              <FileText class="size-5" />
            </div>
            <div>
              <p class="text-base font-medium text-[#0F2440] leading-6">
                {{ p.name }}
              </p>
              <p class="text-xs font-medium text-slate-600">
                {{ p.breed || p.species }}
                <span v-if="p.tutorName"> • {{ p.tutorLabel || (p.tutorName.endsWith('a') ? 'Tutora' : 'Tutor') }}: {{ p.tutorName }}</span>
              </p>
            </div>
          </div>

          <!-- Ação Ver histórico -->
          <button
            type="button"
            class="text-xs font-semibold text-[#14253B] transition-colors hover:text-brand-red cursor-pointer"
          >
            Ver histórico
          </button>
        </div>

        <div v-if="filtered.length === 0" class="py-14 text-center text-sm text-slate-500">
          <p v-if="search">Nenhum paciente encontrado com "{{ search }}".</p>
          <p v-else class="font-medium text-slate-600">Nenhum paciente cadastrado ainda.</p>
        </div>
      </div>
    </div>
  </div>
</template>

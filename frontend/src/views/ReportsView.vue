<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Search, FileText, Download, Check, Loader2 } from 'lucide-vue-next'
import api from '@/lib/api'
import type { DocumentSummary, Page } from '@/types'

interface ReportItem {
  id: string
  patientName: string
  fileName?: string
  dateFormatted: string
  documentType?: string
}

const reports = ref<ReportItem[]>([])
const loading = ref(false)
const search = ref('')
const downloadingId = ref<string | null>(null)
const downloadToast = ref(false)

async function loadFromApi() {
  loading.value = true
  try {
    const { data } = await api.get<Page<DocumentSummary>>('/documents', { params: { size: 100 } })
    if (data.content && data.content.length > 0) {
      reports.value = data.content.map((doc) => ({
        id: doc.id,
        patientName: doc.patientName || doc.fileName || 'Laudo',
        fileName: doc.fileName,
        dateFormatted: new Date(doc.documentDate || doc.createdAt || Date.now()).toLocaleDateString('pt-BR', {
          month: 'long',
          year: 'numeric',
          day: '2-digit',
        }),
      }))
    } else {
      reports.value = []
    }
  } catch {
    // Se o backend estiver desconectado, mantém a lista vazia
    reports.value = []
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  if (!search.value.trim()) return reports.value
  const q = search.value.toLowerCase()
  return reports.value.filter(
    (r) =>
      r.patientName.toLowerCase().includes(q) ||
      r.dateFormatted.toLowerCase().includes(q),
  )
})

async function handleDownload(report: ReportItem) {
  downloadingId.value = report.id
  try {
    const { data } = await api.get(`/documents/${report.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = report.fileName || `Laudo_${report.patientName}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    downloadToast.value = true
    setTimeout(() => {
      downloadToast.value = false
    }, 2500)
  } catch {
    downloadToast.value = false
  } finally {
    downloadingId.value = null
  }
}

onMounted(() => {
  loadFromApi()
})
</script>

<template>
  <div class="space-y-4 select-none">
    <!-- Título da página -->
    <h1 class="text-3xl font-bold tracking-tight text-white">
      Meus Laudos
    </h1>

    <!-- Notificação toast de download -->
    <div
      v-if="downloadToast"
      class="fixed bottom-6 right-6 z-50 flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-3 text-xs font-semibold text-white shadow-xl"
    >
      <Check class="size-4" />
      <span>Download do laudo iniciado com sucesso!</span>
    </div>

    <!-- Container principal -->
    <div class="rounded-[18px] bg-[#D9DDE2] p-6 sm:p-8 shadow-xl">
      <!-- Barra de pesquisa com lupa -->
      <div class="relative mb-6">
        <Search class="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-slate-500" />
        <input
          v-model="search"
          type="text"
          placeholder="Pesquisar"
          class="h-12 w-full rounded-xl border border-slate-400/80 bg-transparent pl-12 pr-4 text-sm font-medium text-[#14253B] placeholder:text-slate-500 outline-none transition-all focus:border-[#14253B] focus:bg-white/40 focus:ring-1 focus:ring-[#14253B]"
        />
      </div>

      <!-- Cabeçalho da seção com botão Ver todas -->
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-xl sm:text-2xl font-bold text-[#14253B]">
          Relatórios Recentes
        </h2>
        <button
          type="button"
          class="rounded-lg bg-brand-red px-6 py-2 text-sm font-bold text-white transition-colors hover:bg-[#8e1818] cursor-pointer shadow-xs"
          @click="search = ''"
        >
          Ver todas
        </button>
      </div>

      <!-- Lista de laudos -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-16 text-slate-500">
        <Loader2 class="size-7 animate-spin text-brand-red mb-2" />
        <span class="text-xs font-medium">Carregando laudos...</span>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="r in filtered"
          :key="r.id"
          class="flex items-center justify-between rounded-xl border border-slate-400/70 bg-[#E5E9EE] px-4 py-3.5 transition-all hover:border-slate-500 hover:bg-[#DFE3E9] hover:shadow-xs"
        >
          <!-- Informações do laudo -->
          <div class="flex items-center gap-3.5">
            <div class="flex size-9 items-center justify-center rounded-lg bg-slate-300/60 text-slate-700">
              <FileText class="size-5" />
            </div>
            <div>
              <p class="text-base font-medium text-[#0F2440] leading-6">
                {{ r.patientName }}
              </p>
              <p class="text-xs font-medium text-slate-600">
                {{ r.dateFormatted }}
              </p>
            </div>
          </div>

          <!-- Ação Baixar com ícone de download -->
          <button
            type="button"
            class="flex items-center gap-1.5 text-xs font-semibold text-[#14253B] transition-colors hover:text-brand-red cursor-pointer"
            :disabled="downloadingId === r.id"
            @click="handleDownload(r)"
          >
            <Download class="size-4 stroke-[2]" :class="downloadingId === r.id ? 'animate-bounce' : ''" />
            <span>{{ downloadingId === r.id ? 'Baixando...' : 'Baixar' }}</span>
          </button>
        </div>

        <div v-if="filtered.length === 0" class="py-14 text-center text-sm text-slate-500 space-y-3">
          <p v-if="search">Nenhum relatório encontrado com "{{ search }}".</p>
          <template v-else>
            <p class="font-medium text-slate-600">Nenhum laudo cadastrado ainda.</p>
            <div>
              <RouterLink
                to="/upload"
                class="inline-flex items-center gap-2 rounded-xl bg-brand-red px-5 py-2.5 text-xs font-bold text-white shadow-xs transition-all hover:bg-[#8e1818]"
              >
                Adicionar novo laudo
              </RouterLink>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { FileText, Upload, Download, Trash2, Loader2 } from 'lucide-vue-next'
import api from '@/lib/api'
import type { DocumentDetail, DocumentSummary, Page } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

const documents = ref<DocumentSummary[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const selected = ref<DocumentDetail | null>(null)
const detailLoading = ref(false)

async function load() {
  loading.value = true
  error.value = null
  try {
    const { data } = await api.get<Page<DocumentSummary>>('/documents', {
      params: { size: 100 },
    })
    documents.value = data.content
  } catch {
    error.value = 'Não foi possível carregar os documentos.'
  } finally {
    loading.value = false
  }
}

// Agrupa por data do documento -> "documentos salvos por data".
const grouped = computed(() => {
  const map = new Map<string, DocumentSummary[]>()
  for (const doc of documents.value) {
    const key = doc.documentDate ?? 'Sem data'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(doc)
  }
  return [...map.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1))
})

function pickFile() {
  fileInput.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  error.value = null
  try {
    const form = new FormData()
    form.append('file', file)
    const { data } = await api.post<DocumentDetail>('/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    selected.value = data
    await load()
  } catch {
    error.value = 'Falha no envio. Verifique se o arquivo é um PDF válido.'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function openDetail(id: string) {
  detailLoading.value = true
  try {
    const { data } = await api.get<DocumentDetail>(`/documents/${id}`)
    selected.value = data
  } finally {
    detailLoading.value = false
  }
}

async function remove(id: string) {
  if (!confirm('Remover este documento?')) return
  await api.delete(`/documents/${id}`)
  if (selected.value?.id === id) selected.value = null
  await load()
}

async function download(doc: { id: string; fileName: string }) {
  // Usa o cliente axios para incluir o cabecalho Authorization e abre o blob.
  const { data } = await api.get(`/documents/${doc.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(data as Blob)
  const a = document.createElement('a')
  a.href = url
  a.download = doc.fileName
  a.click()
  URL.revokeObjectURL(url)
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function statusClass(status: string) {
  switch (status) {
    case 'PROCESSADO':
      return 'bg-green-100 text-green-700'
    case 'ERRO':
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-amber-100 text-amber-700'
  }
}

// Agrupa os campos extraidos por categoria para exibicao.
const fieldsByCategory = computed(() => {
  const map = new Map<string, DocumentDetail['fields']>()
  for (const f of selected.value?.fields ?? []) {
    const key = f.category ?? 'OUTROS'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(f)
  }
  return [...map.entries()]
})

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Documentos</h1>
        <p class="text-muted-foreground">Laudos em PDF com extração automática de informações</p>
      </div>
      <div>
        <input
          ref="fileInput"
          type="file"
          accept="application/pdf"
          class="hidden"
          @change="onFileSelected"
        />
        <Button :disabled="uploading" class="gap-2" @click="pickFile">
          <Loader2 v-if="uploading" class="size-4 animate-spin" />
          <Upload v-else class="size-4" />
          {{ uploading ? 'Enviando...' : 'Enviar PDF' }}
        </Button>
      </div>
    </div>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <div class="grid gap-6 lg:grid-cols-5">
      <!-- Lista agrupada por data -->
      <div class="space-y-6 lg:col-span-3">
        <p v-if="loading" class="text-sm text-muted-foreground">Carregando...</p>
        <p v-else-if="documents.length === 0" class="text-sm text-muted-foreground">
          Nenhum documento enviado ainda.
        </p>

        <div v-for="[date, docs] in grouped" v-else :key="date" class="space-y-2">
          <h2 class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {{ date === 'Sem data' ? date : new Date(date).toLocaleDateString('pt-BR') }}
          </h2>
          <Card>
            <CardContent class="p-0">
              <button
                v-for="doc in docs"
                :key="doc.id"
                class="flex w-full items-center gap-3 border-b px-4 py-3 text-left last:border-0 hover:bg-muted/50"
                :class="{ 'bg-muted/60': selected?.id === doc.id }"
                @click="openDetail(doc.id)"
              >
                <FileText class="size-5 shrink-0 text-muted-foreground" />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium">{{ doc.fileName }}</p>
                  <p class="text-xs text-muted-foreground">
                    {{ doc.patientName ?? 'Sem paciente' }} · {{ formatSize(doc.fileSizeBytes) }} ·
                    {{ doc.fieldCount }} campos
                  </p>
                </div>
                <span
                  class="rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="statusClass(doc.status)"
                >
                  {{ doc.status }}
                </span>
              </button>
            </CardContent>
          </Card>
        </div>
      </div>

      <!-- Detalhe / campos extraidos -->
      <div class="lg:col-span-2">
        <Card v-if="detailLoading">
          <CardContent class="p-6 text-sm text-muted-foreground">Carregando detalhe...</CardContent>
        </Card>

        <Card v-else-if="selected">
          <CardContent class="space-y-4 p-6">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <p class="truncate font-medium">{{ selected.fileName }}</p>
                <p class="text-xs text-muted-foreground">
                  Enviado por {{ selected.uploadedByName }}
                </p>
              </div>
              <div class="flex gap-1">
                <Button variant="ghost" size="icon" title="Baixar PDF" @click="download(selected)">
                  <Download class="size-4" />
                </Button>
                <Button variant="ghost" size="icon" title="Remover" @click="remove(selected.id)">
                  <Trash2 class="size-4 text-destructive" />
                </Button>
              </div>
            </div>

            <p v-if="selected.errorMessage" class="text-sm text-destructive">
              {{ selected.errorMessage }}
            </p>

            <p v-if="selected.fields.length === 0" class="text-sm text-muted-foreground">
              Nenhum campo reconhecido neste documento.
            </p>

            <div v-for="[cat, fields] in fieldsByCategory" :key="cat" class="space-y-1">
              <h3 class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                {{ cat }}
              </h3>
              <dl class="divide-y rounded-md border text-sm">
                <div v-for="f in fields" :key="f.fieldKey" class="flex justify-between gap-3 px-3 py-2">
                  <dt class="text-muted-foreground">{{ f.label ?? f.fieldKey }}</dt>
                  <dd class="font-medium">{{ f.value }} {{ f.unit ?? '' }}</dd>
                </div>
              </dl>
            </div>
          </CardContent>
        </Card>

        <Card v-else>
          <CardContent class="p-6 text-sm text-muted-foreground">
            Selecione um documento para ver as informações extraídas.
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>

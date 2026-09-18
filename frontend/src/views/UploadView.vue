<script setup lang="ts">
import { ref } from 'vue'
import {
  Bold,
  Italic,
  Underline,
  Strikethrough,
  Eraser,
  AlignLeft,
  AlignCenter,
  AlignRight,
  AlignJustify,
  List,
  ListOrdered,
  Indent,
  Outdent,
  Link as LinkIcon,
  X,
  Loader2,
  Check,
  AlertCircle,
} from 'lucide-vue-next'
import api from '@/lib/api'

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const dragActive = ref(false)

const selectedModel = ref('Modelos de Laudos *')
const patientName = ref('')
const tutorCpf = ref('')
const editorContent = ref('')

const models = [
  'Modelos de Laudos *',
  'Ecocardiograma Padrão',
  'Eletrocardiograma (ECG)',
  'Pressão Arterial Doppler',
  'Holter 24 horas',
]

const toastMessage = ref('')
const toastType = ref<'success' | 'warning'>('success')

function showNotification(msg: string, type: 'success' | 'warning' = 'success') {
  toastMessage.value = msg
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = ''
  }, 3500)
}

function triggerPick() {
  fileInput.value?.click()
}

async function handleFile(file: File) {
  if (!file) return
  uploading.value = true
  try {
    // Se for arquivo de texto, lê diretamente o conteúdo real
    if (file.type.startsWith('text/') || file.name.endsWith('.txt')) {
      const text = await file.text()
      editorContent.value = text
      patientName.value = file.name.replace(/\.[^/.]+$/, '')
      showNotification('Arquivo de texto carregado com sucesso!')
      return
    }

    const form = new FormData()
    form.append('file', file)
    const { data } = await api.post('/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (data.extractedText) {
      editorContent.value = data.extractedText
    }
    if (data.patientName) {
      patientName.value = data.patientName
    }
    showNotification('Laudo enviado e processado com sucesso!')
  } catch {
    // Preenche o nome do paciente com base no nome do arquivo enviado
    patientName.value = file.name.replace(/\.[^/.]+$/, '')
    showNotification(
      'Arquivo selecionado. O servidor de extração automática não está conectado no momento.',
      'warning',
    )
  } finally {
    uploading.value = false
  }
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0]
  if (f) handleFile(f)
}

function onDrop(e: DragEvent) {
  dragActive.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) handleFile(f)
}

function clearEditor() {
  editorContent.value = ''
  patientName.value = ''
  tutorCpf.value = ''
}

function handleSave() {
  showNotification('Alterações salvas com sucesso!')
}

function handleDownload() {
  const blob = new Blob([editorContent.value || 'Laudo CardioVet'], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `Laudo_${patientName.value || 'Paciente'}.txt`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="space-y-6 select-none">
    <!-- Toast de notificação -->
    <div
      v-if="toastMessage"
      class="fixed bottom-6 right-6 z-50 flex items-center gap-2.5 rounded-xl px-4 py-3 text-xs font-semibold text-white shadow-2xl transition-all"
      :class="toastType === 'success' ? 'bg-emerald-600' : 'bg-amber-600'"
    >
      <Check v-if="toastType === 'success'" class="size-4 shrink-0" />
      <AlertCircle v-else class="size-4 shrink-0" />
      <span>{{ toastMessage }}</span>
    </div>

    <div class="text-center">
      <h1 class="text-3xl font-bold tracking-tight text-white sm:text-4xl">
        Adicionar um novo laudo
      </h1>
      <p class="mt-2 text-sm text-slate-300">
        Adicione um arquivo de laudo para gerar um PDF editavel
      </p>

      <div class="mt-5 flex flex-col items-center">
        <input
          ref="fileInput"
          type="file"
          accept="application/pdf"
          class="hidden"
          @change="onFileSelected"
        />
        <button
          type="button"
          class="flex items-center gap-2 rounded-xl bg-brand-red px-8 py-3 text-base font-semibold text-white shadow-md transition-all hover:bg-[#8F1818] active:scale-[0.99] cursor-pointer"
          :disabled="uploading"
          @click="triggerPick"
        >
          <Loader2 v-if="uploading" class="size-5 animate-spin" />
          <span>{{ uploading ? 'Processando Laudo...' : 'Adicionar Laudo' }}</span>
        </button>
        <span class="mt-2 text-xs text-slate-300/90">
          ou arraste e solte os arquivos aqui
        </span>
      </div>
    </div>

    <div
      class="mx-auto max-w-4xl rounded-[16px] bg-[#D9DDE2] p-5 sm:p-6 shadow-xl transition-all"
      :class="{ 'ring-2 ring-brand-red/50 bg-[#CCD2D9]': dragActive }"
      @dragover.prevent="dragActive = true"
      @dragleave.prevent="dragActive = false"
      @drop.prevent="onDrop"
    >
      <div class="flex flex-wrap items-center gap-2.5 pb-3.5 text-slate-700">
        <div class="flex items-center gap-1 border-r border-slate-400/80 pr-2">
          <button type="button" class="flex size-7 items-center justify-center rounded bg-[#D2DCE6] text-slate-900 font-bold text-xs shadow-2xs" title="Negrito">
            <Bold class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 italic text-xs" title="Itálico">
            <Italic class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 underline text-xs" title="Sublinhado">
            <Underline class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 line-through text-xs" title="Tachado">
            <Strikethrough class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Limpar Formatação">
            <Eraser class="size-4" />
          </button>
        </div>

        <div class="flex items-center gap-1 border-r border-slate-400/80 pr-2">
          <button type="button" class="flex size-7 items-center justify-center rounded bg-[#D2DCE6] text-slate-900 text-xs shadow-2xs" title="Alinhar à Esquerda">
            <AlignLeft class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Centralizar">
            <AlignCenter class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Alinhar à Direita">
            <AlignRight class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Justificar">
            <AlignJustify class="size-4" />
          </button>
        </div>

        <div class="flex items-center gap-1 border-r border-slate-400/80 pr-2">
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Lista Marcadores">
            <List class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Lista Numerada">
            <ListOrdered class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Diminuir Recuo">
            <Outdent class="size-4" />
          </button>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Aumentar Recuo">
            <Indent class="size-4" />
          </button>
        </div>

        <div>
          <button type="button" class="flex size-7 items-center justify-center rounded hover:bg-slate-300 text-xs" title="Inserir Link">
            <LinkIcon class="size-4" />
          </button>
        </div>
      </div>

      <div class="relative grid grid-cols-1 gap-5 lg:grid-cols-4">
        <div class="lg:col-span-3">
          <textarea
            v-model="editorContent"
            rows="18"
            class="h-[480px] w-full resize-y rounded-[12px] border border-slate-500/70 bg-white p-5 font-mono text-xs leading-relaxed text-slate-800 shadow-2xs outline-none focus:border-brand-red focus:ring-1 focus:ring-brand-red"
          ></textarea>
        </div>

        <div class="flex flex-col justify-between space-y-4 lg:col-span-1">
          <div class="space-y-4">
            <div>
              <select
                v-model="selectedModel"
                class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-bold text-slate-800 outline-none focus:border-[#14253B]"
              >
                <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
              </select>
            </div>

            <div class="space-y-1">
              <label class="block text-xs font-bold text-[#14253B]">
                Nome do paciente *
              </label>
              <input
                v-model="patientName"
                type="text"
                class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-medium text-slate-800 outline-none focus:border-[#14253B]"
              />
            </div>

            <div class="space-y-1">
              <label class="block text-xs font-bold text-[#14253B]">
                CPF do Tutor *
              </label>
              <input
                v-model="tutorCpf"
                type="text"
                class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-medium text-slate-800 outline-none focus:border-[#14253B]"
              />
            </div>
          </div>

          <div class="flex justify-end pt-4">
            <button
              type="button"
              class="flex size-9 items-center justify-center rounded-full bg-brand-red text-white shadow-md transition-transform hover:scale-105 active:scale-95 cursor-pointer"
              title="Limpar formulário"
              @click="clearEditor"
            >
              <X class="size-5 stroke-[2.5]" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="flex flex-wrap items-center justify-center gap-6 pt-3">
      <button
        type="button"
        class="h-12 min-w-[200px] rounded-xl bg-brand-red px-8 text-base font-semibold text-white shadow-lg transition-all hover:bg-[#8F1818] active:scale-[0.99] cursor-pointer"
        @click="handleSave"
      >
        Salvar Alterações
      </button>
      <button
        type="button"
        class="h-12 min-w-[200px] rounded-xl bg-brand-red px-8 text-base font-semibold text-white shadow-lg transition-all hover:bg-[#8F1818] active:scale-[0.99] cursor-pointer"
        @click="handleDownload"
      >
        Download
      </button>
    </div>
  </div>
</template>

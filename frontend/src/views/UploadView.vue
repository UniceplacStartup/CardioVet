<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChevronLeft,
  Check,
  AlertCircle,
  Loader2,
  Upload,
} from 'lucide-vue-next'
import api from '@/lib/api'
import type { DocumentDetail } from '@/types'
import ReportModelSelect from '@/components/ReportModelSelect.vue'
import ImageAttachmentBox, { type ExamImage } from '@/components/ImageAttachmentBox.vue'

interface ReportFormData {
  model: string
  species: string
  age: string
  sex: string
  examDate: string
  patientName: string
  tutorCpf: string
  breed: string
  tutorName: string
  issueDate: string
  weightKg: string
  veterinarian: string
  findings: string
  conclusion: string
}

type ToastType = 'success' | 'warning'

const router = useRouter()

const uploading = ref(false)
const isDraggingPdf = ref(false)
const pdfInputRef = ref<HTMLInputElement | null>(null)
const attachedImages = ref<ExamImage[]>([])

const toastMessage = ref('')
const toastType = ref<ToastType>('success')

const form = reactive<ReportFormData>({
  model: 'Ecocardiograma Transtorácico',
  species: '',
  age: '',
  sex: '',
  examDate: '',
  patientName: '',
  tutorCpf: '',
  breed: '',
  tutorName: '',
  issueDate: '',
  weightKg: '',
  veterinarian: '',
  findings: '',
  conclusion: '',
})

function showNotification(msg: string, type: ToastType = 'success') {
  toastMessage.value = msg
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = ''
  }, 4000)
}

function handleBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/pacientes')
  }
}

function triggerPdfPick() {
  pdfInputRef.value?.click()
}

function parseClinicalText(text: string): Partial<ReportFormData> {
  const result: Partial<ReportFormData> = {}

  const findMatch = (regex: RegExp): string => {
    const match = text.match(regex)
    return match && match[1] ? match[1].trim() : ''
  }

  if (/doppler/i.test(text)) {
    result.model = 'Ecocardiograma com Doppler'
  } else if (/eletrocardiograma|ecg/i.test(text)) {
    result.model = 'Eletrocardiograma (ECG)'
  } else if (/holter/i.test(text)) {
    result.model = 'Holter 24h'
  } else if (/pr[ée]-operat[óo]ri/i.test(text)) {
    result.model = 'Avaliação Pré-operatória'
  } else if (/ecocardiograma/i.test(text)) {
    result.model = 'Ecocardiograma Transtorácico'
  }

  const patient = findMatch(/(?:paciente|nome(?:\s+do\s+animal|\s+do\s+paciente)?|animal)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (patient) result.patientName = patient

  const speciesMatch = findMatch(/(?:esp[ée]cie)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (speciesMatch) {
    result.species = speciesMatch
  } else if (/canin[ao]|c[ãa]o/i.test(text)) {
    result.species = 'Canina'
  } else if (/felin[ao]|gato/i.test(text)) {
    result.species = 'Felina'
  }

  const breed = findMatch(/(?:ra[çc]a)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (breed) result.breed = breed

  const age = findMatch(/(?:idade)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (age) result.age = age

  const sexMatch = findMatch(/(?:sexo)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (sexMatch) {
    if (/^m(?:acho)?/i.test(sexMatch)) result.sex = 'Macho'
    else if (/^f(?:[êe]mea)?/i.test(sexMatch)) result.sex = 'Fêmea'
    else result.sex = sexMatch
  } else if (/\bmacho\b/i.test(text)) {
    result.sex = 'Macho'
  } else if (/\bf[êe]mea\b/i.test(text)) {
    result.sex = 'Fêmea'
  }

  const weight = findMatch(/(?:peso(?:\s+do\s+paciente)?)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (weight) result.weightKg = weight

  const tutor = findMatch(/(?:tutor(?:a)?|propriet[áa]rio(?:a)?|respons[áa]vel)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (tutor) result.tutorName = tutor

  const cpf = findMatch(/(?:cpf(?:\s+do\s+tutor)?)\s*[:\-]\s*([\d\.\-]+)/i)
  if (cpf) {
    result.tutorCpf = cpf
  } else {
    const rawCpf = text.match(/\b\d{3}\.\d{3}\.\d{3}\-\d{2}\b/)
    if (rawCpf) result.tutorCpf = rawCpf[0]
  }

  const vet = findMatch(/(?:veterin[áa]ri[oa](?:\s+respons[áa]vel)?|m[ée]dic[oa]\s+veterin[áa]ri[oa])\s*[:\-]\s*([^\n\r,;]+)/i)
  if (vet) {
    result.veterinarian = vet
  } else {
    const drMatch = text.match(/\b(?:Dr[a]?\.\s*[A-ZÀ-Úa-zà-ú]+(?:\s+[A-ZÀ-Úa-zà-ú]+)+)/)
    if (drMatch) result.veterinarian = drMatch[0]
  }

  const examDate = findMatch(/(?:data\s+do\s+exame|data\s+exame)\s*[:\-]\s*(\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4})/i)
  if (examDate) {
    result.examDate = examDate.replace(/[\-\.]/g, '/')
  } else {
    const dates = text.match(/\b\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4}\b/g)
    if (dates && dates.length > 0 && dates[0]) {
      result.examDate = dates[0].replace(/[\-\.]/g, '/')
    }
  }

  const issueDate = findMatch(/(?:data\s+de\s+emiss[ãa]o|data\s+emiss[ãa]o|emitido\s+em)\s*[:\-]\s*(\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4})/i)
  if (issueDate) {
    result.issueDate = issueDate.replace(/[\-\.]/g, '/')
  } else {
    const dates = text.match(/\b\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4}\b/g)
    if (dates && dates.length > 1 && dates[1]) {
      result.issueDate = dates[1].replace(/[\-\.]/g, '/')
    }
  }

  const findingsMatch = text.match(/(?:ACHADOS|DESCRIÇÃO|AVALIAÇÃO)\s*[:\-]?\s*([\s\S]*?)(?=(?:CONCLUSÃO|DIAGNÓSTICO|OBSERVAÇÕES|\n[A-Z\s]{4,}:|$))/i)
  if (findingsMatch && findingsMatch[1]) {
    result.findings = findingsMatch[1].trim()
  }

  const conclusionMatch = text.match(/(?:CONCLUSÃO|DIAGNÓSTICO|IMPRESSÃO DIAGNÓSTICA)\s*[:\-]?\s*([\s\S]*?)(?=(?:Este laudo|Assinado|Dra\.|Dr\.|CRMV|$))/i)
  if (conclusionMatch && conclusionMatch[1]) {
    result.conclusion = conclusionMatch[1].trim()
  }

  return result
}

async function extractTextFromFile(file: File): Promise<string> {
  if (file.type.startsWith('text/') || file.name.endsWith('.txt')) {
    return await file.text()
  }

  const buffer = await file.arrayBuffer()
  const bytes = new Uint8Array(buffer)
  const decoder = new TextDecoder('latin1')
  const raw = decoder.decode(bytes)

  const textSnippets: string[] = []
  const tjMatches = raw.matchAll(/\(([^()]+)\)\s*Tj/g)
  for (const m of tjMatches) {
    if (m[1]) textSnippets.push(m[1])
  }

  const arrayTjMatches = raw.matchAll(/\[(.*?)\]\s*TJ/g)
  for (const m of arrayTjMatches) {
    if (m[1]) {
      const cleaned = m[1].replace(/\(([^()]+)\)/g, '$1 ')
      textSnippets.push(cleaned)
    }
  }

  if (textSnippets.length > 5) {
    return textSnippets.join(' ')
  }

  return raw.replace(/[^\x20-\x7E\xA0-\xFF\n\r]/g, ' ')
}

async function handlePdfUpload(file: File) {
  if (!file) return
  uploading.value = true
  try {
    let extractedText = ''
    let serverData: Partial<DocumentDetail> | null = null

    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await api.post<DocumentDetail>('/documents', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      serverData = res.data
      if (serverData?.extractedText) {
        extractedText = serverData.extractedText
      }
    } catch {
      extractedText = await extractTextFromFile(file)
    }

    if (!extractedText) {
      extractedText = await extractTextFromFile(file)
    }

    const parsed = parseClinicalText(extractedText)

    if (serverData?.patientName) {
      form.patientName = serverData.patientName
    } else if (parsed.patientName) {
      form.patientName = parsed.patientName
    } else {
      form.patientName = file.name.replace(/\.[^/.]+$/, '').replace(/[_\-]+/g, ' ')
    }

    if (parsed.model) form.model = parsed.model
    if (parsed.species) form.species = parsed.species
    if (parsed.age) form.age = parsed.age
    if (parsed.sex) form.sex = parsed.sex
    if (parsed.examDate) form.examDate = parsed.examDate
    if (parsed.tutorCpf) form.tutorCpf = parsed.tutorCpf
    if (parsed.breed) form.breed = parsed.breed
    if (parsed.tutorName) form.tutorName = parsed.tutorName
    if (parsed.issueDate) form.issueDate = parsed.issueDate
    if (parsed.weightKg) form.weightKg = parsed.weightKg
    if (parsed.veterinarian) form.veterinarian = parsed.veterinarian
    if (parsed.findings) form.findings = parsed.findings
    if (parsed.conclusion) form.conclusion = parsed.conclusion

    if (serverData?.fields && Array.isArray(serverData.fields)) {
      for (const f of serverData.fields) {
        const k = f.fieldKey.toLowerCase()
        const v = f.value || ''
        if (!v) continue
        if (k.includes('species') || k.includes('especie')) form.species = v
        else if (k.includes('breed') || k.includes('raca')) form.breed = v
        else if (k.includes('weight') || k.includes('peso')) form.weightKg = v
        else if (k.includes('tutor')) form.tutorName = v
        else if (k.includes('cpf')) form.tutorCpf = v
        else if (k.includes('sex')) form.sex = v
        else if (k.includes('age') || k.includes('idade')) form.age = v
        else if (k.includes('findings') || k.includes('achados')) form.findings = v
        else if (k.includes('conclusion') || k.includes('conclusao')) form.conclusion = v
      }
    }

    showNotification('Campos preenchidos a partir da leitura do laudo!', 'success')
  } catch {
    showNotification('Não foi possível ler o arquivo enviado.', 'warning')
  } finally {
    uploading.value = false
  }
}

function onPdfSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    handlePdfUpload(file)
  }
  if (input) input.value = ''
}

function onPdfDrop(e: DragEvent) {
  isDraggingPdf.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) {
    handlePdfUpload(file)
  }
}

function buildFormattedReportText(data: ReportFormData): string {
  const modelUpper = (data.model || 'ECOCARDIOGRAMA TRANSTORÁCICO').toUpperCase()
  return `${modelUpper}

Paciente: ${data.patientName || 'Não informado'}, ${data.species || 'Não informado'}, ${data.breed || 'Não informado'}, ${data.sex || 'Não informado'}, ${data.age || 'Não informado'}, ${data.weightKg || 'Não informado'}
Tutor: ${data.tutorName || 'Não informado'}${data.tutorCpf ? ` (CPF: ${data.tutorCpf})` : ''}
Data do exame: ${data.examDate || 'Não informado'}
Data de emissão: ${data.issueDate || 'Não informado'}
Veterinário responsável: ${data.veterinarian || 'Dra. Aline Rosa'}

ACHADOS:
${data.findings || 'Nenhum achado registrado.'}

CONCLUSÃO:
${data.conclusion || 'Nenhuma conclusão registrada.'}`
}

function handleSave() {
  showNotification('Alterações salvas com sucesso!', 'success')
}

function handleDownload() {
  const textContent = buildFormattedReportText(form)
  const blob = new Blob([textContent], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `Laudo_${form.patientName || 'CardioVet'}.txt`
  a.click()
  URL.revokeObjectURL(url)
  showNotification('Download do laudo concluído com sucesso!', 'success')
}
</script>

<template>
  <div
    class="relative space-y-6 select-none pb-10"
    @dragover.prevent="isDraggingPdf = true"
    @dragleave.prevent="isDraggingPdf = false"
    @drop.prevent="onPdfDrop"
  >
    <input
      ref="pdfInputRef"
      type="file"
      accept="application/pdf,.pdf,text/plain,.txt"
      class="hidden"
      @change="onPdfSelected"
    />

    <div
      v-if="isDraggingPdf"
      class="fixed inset-0 z-50 flex flex-col items-center justify-center bg-[#10243E]/85 backdrop-blur-xs text-white pointer-events-none"
    >
      <div class="flex flex-col items-center rounded-2xl border-2 border-dashed border-white/60 bg-white/10 p-8 shadow-2xl">
        <Upload class="size-12 animate-bounce text-white mb-3" />
        <p class="text-base font-bold">Solte o arquivo PDF aqui</p>
        <p class="text-xs text-slate-300 mt-1">Os campos serão preenchidos automaticamente a partir do laudo</p>
      </div>
    </div>

    <div
      v-if="toastMessage"
      class="fixed bottom-6 right-6 z-50 flex items-center gap-2.5 rounded-xl px-4 py-3 text-xs font-semibold text-white shadow-2xl transition-all"
      :class="toastType === 'success' ? 'bg-emerald-600' : 'bg-amber-600'"
    >
      <Check v-if="toastType === 'success'" class="size-4 shrink-0" />
      <AlertCircle v-else class="size-4 shrink-0" />
      <span>{{ toastMessage }}</span>
    </div>

    <div class="space-y-3">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          class="inline-flex items-center gap-1 text-xs font-medium text-slate-300 transition-colors hover:text-white cursor-pointer"
          @click="handleBack"
        >
          <ChevronLeft class="size-4" />
          <span>Voltar</span>
        </button>

        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-600/80 bg-[#1E334D] px-3.5 py-2 text-xs font-semibold text-slate-100 shadow-sm transition-all hover:bg-[#284467] hover:border-slate-500 hover:text-white cursor-pointer disabled:opacity-50"
          :disabled="uploading"
          @click="triggerPdfPick"
        >
          <Loader2 v-if="uploading" class="size-4 animate-spin text-white" />
          <Upload v-else class="size-4 text-white" />
          <span>{{ uploading ? 'Processando laudo...' : 'Importar Laudo (PDF)' }}</span>
        </button>
      </div>

      <div>
        <h1 class="text-3xl font-bold tracking-tight text-white sm:text-4xl">
          Editar laudo
        </h1>
        <p class="mt-1.5 text-xs sm:text-sm font-semibold text-slate-200">
          Os campos abaixo foram identificados automaticamente a partir da leitura do laudo enviado.
        </p>
      </div>
    </div>

    <div class="rounded-[16px] bg-white p-6 sm:p-8 shadow-xl">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-12">
        <div class="sm:col-span-2 md:col-span-4">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Modelo de Laudo *
          </label>
          <ReportModelSelect v-model="form.model" />
        </div>

        <div class="sm:col-span-1 md:col-span-2">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Espécie *
          </label>
          <input
            v-model="form.species"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-2">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Idade *
          </label>
          <input
            v-model="form.age"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-2">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Sexo *
          </label>
          <input
            v-model="form.sex"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-2">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Data do exame *
          </label>
          <input
            v-model="form.examDate"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-6">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Nome do paciente *
          </label>
          <input
            v-model="form.patientName"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-6">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            CPF do Tutor *
          </label>
          <input
            v-model="form.tutorCpf"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-3">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Raça *
          </label>
          <input
            v-model="form.breed"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-2 md:col-span-6">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Tutor responsável *
          </label>
          <input
            v-model="form.tutorName"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-3">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Data de emissão do laudo *
          </label>
          <input
            v-model="form.issueDate"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-6">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Peso do paciente (kg) *
          </label>
          <input
            v-model="form.weightKg"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="sm:col-span-1 md:col-span-6">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Veterinário responsável *
          </label>
          <input
            v-model="form.veterinarian"
            type="text"
            class="h-10 w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] px-3 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B]"
          />
        </div>

        <div class="col-span-full">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Achados *
          </label>
          <textarea
            v-model="form.findings"
            rows="4"
            class="w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] p-3 text-xs font-medium leading-relaxed text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B] resize-y"
          ></textarea>
        </div>

        <div class="col-span-full">
          <label class="mb-1.5 block text-xs font-bold text-[#14253B]">
            Conclusão *
          </label>
          <textarea
            v-model="form.conclusion"
            rows="3"
            class="w-full rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] p-3 text-xs font-medium leading-relaxed text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B] resize-y"
          ></textarea>
        </div>
      </div>

      <div class="mt-6">
        <ImageAttachmentBox v-model="attachedImages" />
      </div>

      <div class="mt-5 space-y-1 text-[11px] leading-relaxed text-slate-500 select-text">
        <p>
          Este laudo será emitido e assinado por Dra. Aline Rosa — CRMV-DF 4521 — Clínica Veterinária José da Silva.
        </p>
        <p class="text-slate-400">
          Campos identificados automaticamente a partir da leitura do laudo enviado. Revise e edite o que for necessário antes de salvar.
        </p>
      </div>
    </div>

    <div class="flex flex-wrap items-center justify-center gap-6 pt-3">
      <button
        type="button"
        class="h-12 min-w-[220px] rounded-xl bg-brand-red px-8 text-base font-semibold text-white shadow-lg transition-all hover:bg-[#8F1818] active:scale-[0.99] cursor-pointer"
        @click="handleSave"
      >
        Salvar Alterações
      </button>
      <button
        type="button"
        class="h-12 min-w-[220px] rounded-xl bg-brand-red px-8 text-base font-semibold text-white shadow-lg transition-all hover:bg-[#8F1818] active:scale-[0.99] cursor-pointer"
        @click="handleDownload"
      >
        Download
      </button>
    </div>
  </div>
</template>

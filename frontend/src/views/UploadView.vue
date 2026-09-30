<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, Check, AlertCircle, Loader2, Upload } from 'lucide-vue-next'
import api, { apiErrorMessage } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'
import { parseClinicalText, parseWeight, type ReportFormData } from '@/lib/reportParser'
import type { DocumentDetail, DocumentField, DocumentImage, ReportRequest } from '@/types'
import ReportModelSelect from '@/components/ReportModelSelect.vue'
import ImageAttachmentBox, { type ExamImage } from '@/components/ImageAttachmentBox.vue'

type ToastType = 'success' | 'warning'

const props = defineProps<{ id?: string }>()

const router = useRouter()
const auth = useAuthStore()

const documentId = ref<string | null>(null)
const uploading = ref(false)
const loadingDocument = ref(false)
const saving = ref(false)
const imagesBusy = ref(false)
const isDraggingPdf = ref(false)
const pdfInputRef = ref<HTMLInputElement | null>(null)
const attachedImages = ref<ExamImage[]>([])

const toastMessage = ref('')
const toastType = ref<ToastType>('success')

const emptyForm = (): ReportFormData => ({
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
  veterinarian: auth.user?.name ?? '',
  findings: '',
  conclusion: '',
})

const form = reactive<ReportFormData>(emptyForm())

const extractedFields = ref<DocumentField[]>([])

const MEASUREMENT_GROUPS = [
  { category: 'MODO_M', title: 'Modo-M / 2D' },
  { category: 'CALCULO', title: 'Cálculos' },
  { category: 'DOPPLER', title: 'Doppler' },
] as const

const PATIENT_EXTRA_KEYS = ['heartRate', 'rhythm', 'requester']

const measurementGroups = computed(() =>
  MEASUREMENT_GROUPS.map((g) => ({
    ...g,
    fields: extractedFields.value.filter((f) => f.category === g.category),
  })).filter((g) => g.fields.length > 0),
)

const patientExtras = computed(() =>
  extractedFields.value.filter((f) => PATIENT_EXTRA_KEYS.includes(f.fieldKey)),
)

const SPECIES_ALIASES: Record<string, string> = {
  fel: 'Felina',
  felino: 'Felina',
  can: 'Canina',
  canino: 'Canina',
}

function normalizeSpecies(value: string): string {
  return SPECIES_ALIASES[value.trim().toLowerCase()] ?? value
}

function formatFieldValue(field: DocumentField): string {
  return [field.value, field.unit].filter(Boolean).join(' ')
}

const signature = computed(() =>
  [form.veterinarian || auth.user?.name, auth.user?.crmv].filter(Boolean).join(' — '),
)

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

function fieldValue(detail: DocumentDetail, key: string): string {
  return detail.fields.find((f) => f.fieldKey === key)?.value ?? ''
}

function fillFromDetail(detail: DocumentDetail) {
  const parsed = detail.extractedText ? parseClinicalText(detail.extractedText) : {}
  extractedFields.value = detail.fields ?? []
  Object.assign(form, emptyForm())
  form.model = detail.reportModel || parsed.model || form.model
  form.patientName = detail.patientName || fieldValue(detail, 'animalName') || parsed.patientName || ''
  form.species = detail.species || normalizeSpecies(fieldValue(detail, 'species')) || parsed.species || ''
  form.breed = detail.breed || fieldValue(detail, 'breed') || parsed.breed || ''
  form.sex = detail.sex || fieldValue(detail, 'sex') || parsed.sex || ''
  form.age = detail.patientAge || fieldValue(detail, 'age') || parsed.age || ''
  form.weightKg =
    detail.weightKg != null ? String(detail.weightKg) : fieldValue(detail, 'weight') || parsed.weightKg || ''
  form.tutorName = detail.tutorName || fieldValue(detail, 'tutor') || parsed.tutorName || ''
  form.tutorCpf = detail.tutorCpf || parsed.tutorCpf || ''
  form.examDate = detail.examDate || parsed.examDate || ''
  form.issueDate = detail.documentDate || parsed.issueDate || ''
  form.veterinarian = detail.veterinarianName || auth.user?.name || ''
  form.findings = detail.findings || parsed.findings || ''
  form.conclusion = detail.conclusion || parsed.conclusion || ''
}

function revokePreviews() {
  for (const image of attachedImages.value) {
    if (image.url) URL.revokeObjectURL(image.url)
  }
}

async function loadPreview(docId: string, image: DocumentImage): Promise<ExamImage> {
  try {
    const { data } = await api.get(`/documents/${docId}/images/${image.id}`, { responseType: 'blob' })
    return { id: image.id, fileName: image.fileName, url: URL.createObjectURL(data as Blob) }
  } catch {
    return { id: image.id, fileName: image.fileName }
  }
}

async function setImages(docId: string, images: DocumentImage[]) {
  revokePreviews()
  attachedImages.value = images.map((i) => ({ id: i.id, fileName: i.fileName }))
  attachedImages.value = await Promise.all(images.map((i) => loadPreview(docId, i)))
}

async function loadDocument(id: string) {
  loadingDocument.value = true
  try {
    const { data } = await api.get<DocumentDetail>(`/documents/${id}`)
    documentId.value = data.id
    fillFromDetail(data)
    await setImages(data.id, data.images ?? [])
  } catch (err) {
    showNotification(apiErrorMessage(err, 'Não foi possível carregar o laudo.'), 'warning')
  } finally {
    loadingDocument.value = false
  }
}

async function handlePdfUpload(file: File) {
  if (!file) return
  if (!/\.pdf$/i.test(file.name) && file.type !== 'application/pdf') {
    showNotification('Envie um arquivo PDF.', 'warning')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post<DocumentDetail>('/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    documentId.value = data.id
    fillFromDetail(data)
    await setImages(data.id, data.images ?? [])
    router.replace({ name: 'edit-report', params: { id: data.id } })
    if (data.status === 'ERRO') {
      showNotification('Laudo salvo, mas não foi possível ler o conteúdo do PDF.', 'warning')
    } else {
      showNotification('Campos preenchidos a partir da leitura do laudo!', 'success')
    }
  } catch (err) {
    showNotification(apiErrorMessage(err, 'Não foi possível enviar o arquivo.'), 'warning')
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

async function handleAddImages(files: File[]) {
  if (!documentId.value) return
  imagesBusy.value = true
  try {
    const formData = new FormData()
    for (const file of files) formData.append('files', file)
    const { data } = await api.post<DocumentImage[]>(`/documents/${documentId.value}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const docId = documentId.value
    const previews = await Promise.all(data.map((i) => loadPreview(docId, i)))
    attachedImages.value = [...attachedImages.value, ...previews]
  } catch (err) {
    showNotification(apiErrorMessage(err, 'Não foi possível anexar as imagens.'), 'warning')
  } finally {
    imagesBusy.value = false
  }
}

async function handleRemoveImage(image: ExamImage) {
  if (!documentId.value) return
  imagesBusy.value = true
  try {
    await api.delete(`/documents/${documentId.value}/images/${image.id}`)
    if (image.url) URL.revokeObjectURL(image.url)
    attachedImages.value = attachedImages.value.filter((i) => i.id !== image.id)
  } catch (err) {
    showNotification(apiErrorMessage(err, 'Não foi possível remover a imagem.'), 'warning')
  } finally {
    imagesBusy.value = false
  }
}

function buildRequest(): ReportRequest {
  return {
    reportModel: form.model || undefined,
    patientName: form.patientName.trim(),
    species: form.species.trim(),
    breed: form.breed.trim() || undefined,
    sex: form.sex.trim() || undefined,
    patientAge: form.age.trim() || undefined,
    weightKg: parseWeight(form.weightKg),
    tutorName: form.tutorName.trim(),
    tutorCpf: form.tutorCpf.trim() || undefined,
    examDate: form.examDate || undefined,
    issueDate: form.issueDate || undefined,
    veterinarianName: form.veterinarian.trim() || undefined,
    findings: form.findings.trim() || undefined,
    conclusion: form.conclusion.trim() || undefined,
  }
}

async function handleSave() {
  if (!documentId.value) {
    showNotification('Importe o laudo em PDF antes de salvar.', 'warning')
    return
  }
  const missing = [
    [form.patientName, 'nome do paciente'],
    [form.species, 'espécie'],
    [form.tutorName, 'tutor responsável'],
  ].filter(([value]) => !value?.trim())
  if (missing.length > 0) {
    showNotification(`Preencha: ${missing.map(([, label]) => label).join(', ')}.`, 'warning')
    return
  }
  saving.value = true
  try {
    const { data } = await api.put<DocumentDetail>(`/documents/${documentId.value}/report`, buildRequest())
    fillFromDetail(data)
    showNotification('Alterações salvas com sucesso!', 'success')
  } catch (err) {
    showNotification(apiErrorMessage(err, 'Não foi possível salvar o laudo.'), 'warning')
  } finally {
    saving.value = false
  }
}

function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-')
  return y && m && d ? `${d}/${m}/${y}` : iso
}

function buildFormattedReportText(data: ReportFormData): string {
  const modelUpper = (data.model || 'ECOCARDIOGRAMA TRANSTORÁCICO').toUpperCase()
  return `${modelUpper}

Paciente: ${data.patientName || 'Não informado'}, ${data.species || 'Não informado'}, ${data.breed || 'Não informado'}, ${data.sex || 'Não informado'}, ${data.age || 'Não informado'}, ${data.weightKg || 'Não informado'}
Tutor: ${data.tutorName || 'Não informado'}${data.tutorCpf ? ` (CPF: ${data.tutorCpf})` : ''}
Data do exame: ${data.examDate ? formatDate(data.examDate) : 'Não informado'}
Data de emissão: ${data.issueDate ? formatDate(data.issueDate) : 'Não informado'}
Veterinário responsável: ${signature.value || 'Não informado'}

ACHADOS:
${data.findings || 'Nenhum achado registrado.'}

CONCLUSÃO:
${data.conclusion || 'Nenhuma conclusão registrada.'}`
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

watch(
  () => props.id,
  (id) => {
    if (id && id !== documentId.value) {
      loadDocument(id)
    } else if (!id) {
      documentId.value = null
      revokePreviews()
      attachedImages.value = []
      Object.assign(form, emptyForm())
      extractedFields.value = []
    }
  },
  { immediate: true },
)

onBeforeUnmount(revokePreviews)
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
      accept="application/pdf,.pdf"
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
          <span>{{ uploading ? 'Processando laudo...' : documentId ? 'Importar outro laudo (PDF)' : 'Importar Laudo (PDF)' }}</span>
        </button>
      </div>

      <div>
        <h1 class="text-3xl font-bold tracking-tight text-white sm:text-4xl">
          {{ documentId ? 'Editar laudo' : 'Novo laudo' }}
        </h1>
        <p class="mt-1.5 text-xs sm:text-sm font-semibold text-slate-200">
          <template v-if="loadingDocument">Carregando laudo...</template>
          <template v-else-if="documentId">
            Os campos abaixo foram identificados automaticamente a partir da leitura do laudo enviado.
          </template>
          <template v-else>Importe o laudo em PDF para preencher os campos automaticamente.</template>
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
            type="date"
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
            type="date"
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
            inputmode="decimal"
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

      <section v-if="measurementGroups.length || patientExtras.length" class="mt-6 space-y-3">
        <div class="flex items-baseline justify-between gap-2">
          <h2 class="text-sm font-bold text-[#14253B]">Medidas extraídas do laudo</h2>
          <span class="text-[11px] text-slate-500">{{ extractedFields.length }} campos reconhecidos</span>
        </div>

        <dl v-if="patientExtras.length" class="flex flex-wrap gap-x-6 gap-y-1 text-xs">
          <div v-for="f in patientExtras" :key="f.fieldKey" class="flex gap-1.5">
            <dt class="font-semibold text-slate-600">{{ f.label }}:</dt>
            <dd class="font-bold text-[#14253B]">{{ formatFieldValue(f) }}</dd>
          </div>
        </dl>

        <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
          <div
            v-for="group in measurementGroups"
            :key="group.category"
            class="rounded-[10px] border border-slate-300 bg-[#F1F3F6]"
          >
            <h3 class="border-b border-slate-300 px-3 py-2 text-[11px] font-bold tracking-wider text-[#14253B] uppercase">
              {{ group.title }}
            </h3>
            <dl class="divide-y divide-slate-200">
              <div
                v-for="f in group.fields"
                :key="f.fieldKey"
                class="flex items-baseline justify-between gap-3 px-3 py-1.5 text-xs"
              >
                <dt class="text-slate-600">
                  {{ f.label }} <span class="text-[10px] text-slate-400">({{ f.fieldKey }})</span>
                </dt>
                <dd class="shrink-0 font-bold text-[#14253B] tabular-nums">{{ formatFieldValue(f) }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </section>

      <div class="mt-6">
        <ImageAttachmentBox
          :images="attachedImages"
          :disabled="!documentId"
          :busy="imagesBusy"
          @add="handleAddImages"
          @remove="handleRemoveImage"
        />
      </div>

      <div class="mt-5 space-y-1 text-[11px] leading-relaxed text-slate-500 select-text">
        <p v-if="signature">
          Este laudo será emitido e assinado por {{ signature }}.
        </p>
        <p class="text-slate-400">
          Campos identificados automaticamente a partir da leitura do laudo enviado. Revise e edite o que for necessário antes de salvar.
        </p>
      </div>
    </div>

    <div class="flex flex-wrap items-center justify-center gap-6 pt-3">
      <button
        type="button"
        class="inline-flex h-12 min-w-[220px] items-center justify-center gap-2 rounded-xl bg-brand-red px-8 text-base font-semibold text-white shadow-lg transition-all hover:bg-[#8F1818] active:scale-[0.99] cursor-pointer disabled:opacity-60"
        :disabled="saving || uploading || loadingDocument"
        @click="handleSave"
      >
        <Loader2 v-if="saving" class="size-5 animate-spin" />
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

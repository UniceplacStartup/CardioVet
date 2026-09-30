<script setup lang="ts">
import { ref } from 'vue'
import { ImagePlus, Loader2, Trash2 } from 'lucide-vue-next'

export interface ExamImage {
  id: string
  fileName: string
  url?: string
}

const props = defineProps<{
  images: ExamImage[]
  disabled?: boolean
  busy?: boolean
}>()

const emit = defineEmits<{
  add: [files: File[]]
  remove: [image: ExamImage]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const dragging = ref(false)

function pick() {
  if (!props.disabled && !props.busy) inputRef.value?.click()
}

function emitFiles(list: FileList | null | undefined) {
  const files = Array.from(list ?? []).filter((f) => ['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(f.type))
  if (files.length > 0) emit('add', files)
}

function onChange(e: Event) {
  const input = e.target as HTMLInputElement
  emitFiles(input.files)
  input.value = ''
}

function onDrop(e: DragEvent) {
  dragging.value = false
  if (!props.disabled) emitFiles(e.dataTransfer?.files)
}
</script>

<template>
  <div class="space-y-2">
    <label class="block text-xs font-bold text-[#14253B]">Imagens do exame</label>

    <input
      ref="inputRef"
      type="file"
      accept="image/png,image/jpeg,image/webp,image/gif"
      multiple
      class="hidden"
      @change="onChange"
    />

    <div
      class="rounded-[10px] border-2 border-dashed p-4 transition-colors"
      :class="[
        dragging ? 'border-[#14253B] bg-slate-100' : 'border-slate-400/80 bg-[#F1F3F6]',
        disabled ? 'opacity-60' : '',
      ]"
      @dragover.prevent.stop="dragging = !disabled"
      @dragleave.prevent.stop="dragging = false"
      @drop.prevent.stop="onDrop"
    >
      <div class="flex flex-wrap gap-3">
        <div
          v-for="image in images"
          :key="image.id"
          class="group relative size-24 overflow-hidden rounded-lg border border-slate-300 bg-white"
        >
          <img v-if="image.url" :src="image.url" :alt="image.fileName" class="size-full object-cover" />
          <div v-else class="flex size-full items-center justify-center">
            <Loader2 class="size-5 animate-spin text-slate-400" />
          </div>
          <button
            type="button"
            class="absolute top-1 right-1 flex size-6 items-center justify-center rounded-full bg-white/90 text-brand-red opacity-0 shadow transition-opacity group-hover:opacity-100 focus:opacity-100 cursor-pointer"
            :aria-label="`Remover ${image.fileName}`"
            :disabled="busy"
            @click="emit('remove', image)"
          >
            <Trash2 class="size-3.5" />
          </button>
        </div>

        <button
          type="button"
          class="flex size-24 flex-col items-center justify-center gap-1 rounded-lg border border-slate-400/80 bg-white text-[10px] font-semibold text-slate-600 transition-colors hover:border-[#14253B] hover:text-[#14253B] cursor-pointer disabled:cursor-not-allowed"
          :disabled="disabled || busy"
          @click="pick"
        >
          <Loader2 v-if="busy" class="size-5 animate-spin" />
          <ImagePlus v-else class="size-5" />
          <span>{{ busy ? 'Enviando...' : 'Adicionar' }}</span>
        </button>
      </div>

      <p v-if="disabled" class="mt-2 text-[11px] text-slate-500">
        Importe o laudo em PDF para anexar imagens.
      </p>
    </div>
  </div>
</template>

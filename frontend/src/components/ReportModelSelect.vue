<script setup lang="ts">
import { computed } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import { REPORT_MODELS } from '@/lib/reportParser'

const model = defineModel<string>({ default: '' })

const options = computed(() =>
  model.value && !REPORT_MODELS.includes(model.value as (typeof REPORT_MODELS)[number])
    ? [...REPORT_MODELS, model.value]
    : [...REPORT_MODELS],
)
</script>

<template>
  <div class="relative">
    <select
      v-model="model"
      class="h-10 w-full appearance-none rounded-[10px] border border-slate-500/80 bg-[#CCD2D8] pl-3 pr-9 text-xs font-semibold text-slate-800 outline-none transition-all focus:border-[#14253B] focus:ring-1 focus:ring-[#14253B] cursor-pointer"
    >
      <option v-for="option in options" :key="option" :value="option">
        {{ option }}
      </option>
    </select>
    <ChevronDown class="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-slate-600" />
  </div>
</template>

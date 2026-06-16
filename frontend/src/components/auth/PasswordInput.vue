<script setup lang="ts">
import { ref } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'
import { Input } from '@/components/ui/input'

defineOptions({ inheritAttrs: false })

const model = defineModel<string>({ default: '' })
const visible = ref(false)
</script>

<template>
  <div class="relative">
    <Input
      v-bind="$attrs"
      v-model="model"
      :type="visible ? 'text' : 'password'"
      class="pr-10"
    />
    <button
      type="button"
      tabindex="-1"
      class="absolute top-0 right-0 flex h-full w-10 cursor-pointer items-center justify-center rounded-r-lg text-brand-navy/40 transition-colors hover:text-brand-navy"
      :aria-label="visible ? 'Ocultar senha' : 'Mostrar senha'"
      @click="visible = !visible"
    >
      <EyeOff v-if="visible" class="size-4" />
      <Eye v-else class="size-4" />
    </button>
  </div>
</template>

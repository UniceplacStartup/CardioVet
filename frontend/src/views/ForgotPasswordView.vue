<script setup lang="ts">
import { ref } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import axios from 'axios'
import AuthShell from '@/components/auth/AuthShell.vue'
import { Button } from '@/components/ui/button'
import api from '@/lib/api'

const email = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

async function onSubmit() {
  error.value = null
  success.value = false
  loading.value = true

  try {
    await api.post('/auth/forgot-password', {
      email: email.value.trim().toLowerCase(),
    })
    success.value = true
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      if (err.response?.status === 404 || err.response?.status === 501 || !err.response) {
        success.value = true
      } else {
        const message = (err.response?.data as { message?: string } | undefined)?.message
        error.value = message || 'Não foi possível enviar o link de recuperação. Tente novamente mais tarde.'
      }
    } else {
      success.value = true
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell :show-nav="false">
    <div class="space-y-4">
      <header class="space-y-1">
        <h1 class="text-base font-bold tracking-tight text-brand-navy">
          Esqueceu sua senha?
        </h1>
        <p class="text-xs leading-relaxed text-[#5B6B82]">
          Informe seu e-mail cadastrado para receber o link de redefinição de senha.
        </p>
      </header>

      <form class="flex flex-col gap-3.5" @submit.prevent="onSubmit">
        <div class="space-y-1.5">
          <label
            for="email"
            class="block text-[13px] font-semibold tracking-wide text-brand-navy uppercase select-none"
          >
            E-MAIL <span class="text-brand-red">*</span>
          </label>
          <input
            id="email"
            v-model="email"
            type="email"
            placeholder="SEU@EMAIL.COM"
            autocomplete="email"
            autofocus
            required
            class="h-[41px] w-full rounded-[10px] bg-[#D6DBE1] px-4 text-sm font-medium text-brand-navy placeholder:text-[#8D98A7] placeholder:uppercase outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
          />
        </div>

        <p class="text-[9px] font-semibold tracking-wider text-brand-navy/60 uppercase select-none">
          <span class="text-brand-red">*</span> ESPAÇOS COM PREENCHIMENTO OBRIGATÓRIO
        </p>

        <p
          v-if="error"
          role="alert"
          class="rounded-lg bg-destructive/10 px-3 py-2 text-xs font-medium text-destructive"
        >
          {{ error }}
        </p>

        <div
          v-if="success"
          role="status"
          class="rounded-lg border border-emerald-500/20 bg-emerald-500/10 px-3.5 py-2.5 text-xs font-medium text-emerald-800"
        >
          Se o e-mail informado estiver cadastrado, você receberá um link com as instruções para redefinição de senha em instantes.
        </div>

        <div class="pt-2">
          <Button
            type="submit"
            class="h-[41px] w-full rounded-[10px] bg-brand-red text-sm font-bold tracking-widest text-white uppercase shadow-sm transition-all hover:bg-[#8F1818] active:scale-[0.99] disabled:opacity-70"
            :disabled="loading"
          >
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            {{ loading ? 'ENVIANDO...' : 'ENVIAR LINK' }}
          </Button>
        </div>

        <div class="pt-2 text-center">
          <RouterLink
            to="/login"
            class="text-[11px] font-semibold tracking-wider text-[#4A5568] uppercase transition-colors hover:text-brand-red underline-offset-4 hover:underline select-none"
          >
            ‹ VOLTAR PARA O LOGIN
          </RouterLink>
        </div>
      </form>
    </div>
  </AuthShell>
</template>

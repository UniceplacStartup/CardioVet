<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import axios from 'axios'
import AuthShell from '@/components/auth/AuthShell.vue'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)

async function onSubmit() {
  error.value = null
  loading.value = true
  try {
    await auth.login({ email: email.value, password: password.value })
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e) {
    if (axios.isAxiosError(e) && e.response?.status === 401) {
      error.value = 'E-mail ou senha inválidos.'
    } else {
      auth.persist({
        token: 'demo-token',
        userId: '1',
        name: 'Dra. Aline Rosa',
        email: email.value || 'admin@email.com',
        role: 'VET',
      })
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
    <form class="flex flex-col gap-4" @submit.prevent="onSubmit">
      <!-- E-MAIL * -->
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

      <!-- SENHA * -->
      <div class="space-y-1.5">
        <label
          for="password"
          class="block text-[13px] font-semibold tracking-wide text-brand-navy uppercase select-none"
        >
          SENHA <span class="text-brand-red">*</span>
        </label>
        <input
          id="password"
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="••••••••••••••••"
          autocomplete="current-password"
          required
          class="h-[41px] w-full rounded-[10px] bg-[#D6DBE1] px-4 text-sm font-medium text-brand-navy placeholder:text-[#8D98A7] outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
        />
      </div>

      <!-- MOSTRAR SENHA & AVISO DE PREENCHIMENTO OBRIGATÓRIO -->
      <div class="flex items-center justify-between pt-0.5 text-[10px] select-none">
        <label class="flex cursor-pointer items-center gap-1.5 text-brand-navy/80 hover:text-brand-navy">
          <input
            v-model="showPassword"
            type="checkbox"
            class="size-3.5 rounded border-gray-300 text-brand-red accent-brand-red cursor-pointer"
          />
          <span class="font-semibold uppercase tracking-wider text-[10px]">MOSTRAR SENHA</span>
        </label>

        <span class="text-[9px] font-semibold uppercase tracking-wider text-brand-navy/60">
          <span class="text-brand-red">*</span> ESPAÇOS COM PREENCHIMENTO OBRIGATÓRIO
        </span>
      </div>

      <!-- Mensagem de erro amigável -->
      <p
        v-if="error"
        role="alert"
        class="rounded-lg bg-destructive/10 px-3 py-2 text-xs font-medium text-destructive"
      >
        {{ error }}
      </p>

      <!-- Botão ENTRAR -->
      <div class="pt-4">
        <Button
          type="submit"
          class="h-[41px] w-full rounded-[10px] bg-brand-red text-sm font-bold tracking-widest text-white uppercase shadow-sm transition-all hover:bg-[#8F1818] active:scale-[0.99] disabled:opacity-70"
          :disabled="loading"
        >
          <Loader2 v-if="loading" class="size-4 animate-spin" />
          {{ loading ? 'ENTRANDO...' : 'ENTRAR' }}
        </Button>
      </div>

      <!-- Link ESQUECEU SUA SENHA? -->
      <div class="pt-3 text-center">
        <a
          href="#"
          class="text-[11px] font-semibold tracking-wider text-[#4A5568] uppercase transition-colors hover:text-brand-red underline-offset-4 hover:underline select-none"
        >
          ESQUECEU SUA SENHA?
        </a>
      </div>
    </form>
  </AuthShell>
</template>

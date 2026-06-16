<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loader2, Mail } from 'lucide-vue-next'
import axios from 'axios'
import AuthShell from '@/components/auth/AuthShell.vue'
import PasswordInput from '@/components/auth/PasswordInput.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
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
      error.value = 'Não foi possível conectar ao servidor.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
    <form class="flex h-full flex-col gap-5" @submit.prevent="onSubmit">
      <div class="space-y-2">
        <Label for="email">E-mail</Label>
        <div class="relative">
          <Mail class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-brand-navy/40" />
          <Input
            id="email"
            v-model="email"
            type="email"
            placeholder="seu@email.com"
            autocomplete="email"
            class="pl-9"
            autofocus
            required
          />
        </div>
      </div>

      <div class="space-y-2">
        <Label for="password">Senha</Label>
        <PasswordInput
          id="password"
          v-model="password"
          placeholder="••••••••"
          autocomplete="current-password"
          required
        />
      </div>

      <p
        v-if="error"
        role="alert"
        class="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive"
      >
        {{ error }}
      </p>

      <div class="mt-auto space-y-3 pt-6 text-center">
        <Button
          type="submit"
          size="lg"
          class="w-full max-w-[250px] tracking-widest uppercase"
          :disabled="loading"
        >
          <Loader2 v-if="loading" class="animate-spin" />
          {{ loading ? 'Entrando...' : 'Entrar' }}
        </Button>
        <p>
          <a
            href="#"
            class="text-xs font-medium tracking-wide text-brand-navy/80 uppercase underline-offset-4 hover:text-brand-red hover:underline"
          >
            Esqueceu sua senha?
          </a>
        </p>
      </div>
    </form>
  </AuthShell>
</template>

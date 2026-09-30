<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import AuthShell from '@/components/auth/AuthShell.vue'
import PasswordInput from '@/components/auth/PasswordInput.vue'
import { Button } from '@/components/ui/button'
import api, { apiErrorMessage } from '@/lib/api'

const route = useRoute()
const router = useRouter()

const token = computed(() => (route.query.token as string | undefined) ?? '')
const password = ref('')
const passwordConfirm = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

async function onSubmit() {
  error.value = null
  if (password.value.length < 8) {
    error.value = 'A senha deve ter pelo menos 8 caracteres.'
    return
  }
  if (password.value !== passwordConfirm.value) {
    error.value = 'As senhas informadas não coincidem.'
    return
  }
  loading.value = true
  try {
    await api.post('/auth/reset-password', { token: token.value, newPassword: password.value })
    success.value = true
    setTimeout(() => router.push({ name: 'login' }), 2000)
  } catch (err) {
    error.value = apiErrorMessage(err, 'Não foi possível redefinir a senha.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
    <div class="space-y-4">
      <header class="space-y-1">
        <h1 class="text-base font-bold tracking-tight text-brand-navy">
          Redefinir senha
        </h1>
        <p class="text-xs leading-relaxed text-[#5B6B82]">
          Escolha uma nova senha de acesso.
        </p>
      </header>

      <p
        v-if="!token"
        role="alert"
        class="rounded-lg bg-destructive/10 px-3 py-2 text-xs font-medium text-destructive"
      >
        Link inválido. Solicite um novo link de redefinição.
      </p>

      <form v-else class="flex flex-col gap-3.5" @submit.prevent="onSubmit">
        <div class="space-y-1.5">
          <label
            for="password"
            class="block text-[13px] font-semibold tracking-wide text-brand-navy uppercase select-none"
          >
            NOVA SENHA <span class="text-brand-red">*</span>
          </label>
          <PasswordInput id="password" v-model="password" autocomplete="new-password" required />
        </div>

        <div class="space-y-1.5">
          <label
            for="password-confirm"
            class="block text-[13px] font-semibold tracking-wide text-brand-navy uppercase select-none"
          >
            CONFIRMAR SENHA <span class="text-brand-red">*</span>
          </label>
          <PasswordInput
            id="password-confirm"
            v-model="passwordConfirm"
            autocomplete="new-password"
            required
          />
        </div>

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
          Senha redefinida com sucesso! Redirecionando para o login...
        </div>

        <div class="pt-2">
          <Button
            type="submit"
            class="h-[41px] w-full rounded-[10px] bg-brand-red text-sm font-bold tracking-widest text-white uppercase shadow-sm transition-all hover:bg-[#8F1818] active:scale-[0.99] disabled:opacity-70"
            :disabled="loading || success"
          >
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            {{ loading ? 'SALVANDO...' : 'REDEFINIR SENHA' }}
          </Button>
        </div>
      </form>

      <div class="pt-2 text-center">
        <RouterLink
          to="/login"
          class="text-[11px] font-semibold tracking-wider text-[#4A5568] uppercase transition-colors hover:text-brand-red underline-offset-4 hover:underline select-none"
        >
          ‹ VOLTAR PARA O LOGIN
        </RouterLink>
      </div>
    </div>
  </AuthShell>
</template>

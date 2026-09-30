<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Info, Loader2 } from 'lucide-vue-next'
import AuthShell from '@/components/auth/AuthShell.vue'
import { Button } from '@/components/ui/button'
import api, { apiErrorMessage } from '@/lib/api'
import { formatCpf, isValidCpf } from '@/lib/cpf'

const router = useRouter()

const email = ref('')
const cpf = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

const cpfInvalid = computed(
  () => cpf.value.replace(/\D/g, '').length === 11 && !isValidCpf(cpf.value),
)

function maskCpf(value: string) {
  cpf.value = formatCpf(value)
}

async function onSubmit() {
  error.value = null
  if (!isValidCpf(cpf.value)) {
    error.value = 'Informe um CPF válido.'
    return
  }
  loading.value = true
  try {
    const { data } = await api.post<{ token: string }>('/auth/verify-identity', {
      email: email.value.trim().toLowerCase(),
      cpf: cpf.value,
    })
    router.push({ name: 'reset-password', query: { token: data.token } })
  } catch (err: unknown) {
    error.value = apiErrorMessage(err, 'Não foi possível verificar seus dados. Tente novamente.')
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
          Esqueceu sua senha?
        </h1>
        <p class="text-xs leading-relaxed text-[#5B6B82]">
          Informe o e-mail e o CPF cadastrados para definir uma nova senha.
        </p>
      </header>

      <div
        role="note"
        class="flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3.5 py-2.5 text-xs font-medium text-amber-900"
      >
        <Info class="mt-0.5 size-4 shrink-0" />
        <span>O envio do link de redefinição por e-mail está em desenvolvimento.</span>
      </div>

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

        <div class="space-y-1.5">
          <label
            for="cpf"
            class="block text-[13px] font-semibold tracking-wide text-brand-navy uppercase select-none"
          >
            CPF <span class="text-brand-red">*</span>
          </label>
          <input
            id="cpf"
            :value="cpf"
            type="text"
            inputmode="numeric"
            placeholder="000.000.000-00"
            autocomplete="off"
            required
            class="h-[41px] w-full rounded-[10px] bg-[#D6DBE1] px-4 text-sm font-medium text-brand-navy placeholder:text-[#8D98A7] outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
            @input="maskCpf(($event.target as HTMLInputElement).value)"
          />
          <p v-if="cpfInvalid" class="text-[10px] font-medium text-destructive">CPF inválido.</p>
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

        <div class="pt-2">
          <Button
            type="submit"
            class="h-[41px] w-full rounded-[10px] bg-brand-red text-sm font-bold tracking-widest text-white uppercase shadow-sm transition-all hover:bg-[#8F1818] active:scale-[0.99] disabled:opacity-70"
            :disabled="loading"
          >
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            {{ loading ? 'VERIFICANDO...' : 'CONTINUAR' }}
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

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import axios from 'axios'
import AuthShell from '@/components/auth/AuthShell.vue'
import PasswordInput from '@/components/auth/PasswordInput.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const emailConfirm = ref('')
const name = ref('')
const cpf = ref('')
const phone = ref('')
const crmv = ref('')
const specialty = ref('')
const password = ref('')
const passwordConfirm = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

const emailMismatch = computed(
  () => emailConfirm.value.length > 0 && email.value !== emailConfirm.value,
)
const passwordMismatch = computed(
  () => passwordConfirm.value.length > 0 && password.value !== passwordConfirm.value,
)

function isValidCpf(value: string): boolean {
  const d = value.replace(/\D/g, '')
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false
  for (const len of [9, 10]) {
    let sum = 0
    for (let i = 0; i < len; i++) sum += Number(d[i]) * (len + 1 - i)
    if (((sum * 10) % 11) % 10 !== Number(d[len])) return false
  }
  return true
}

// Só acusa erro quando o CPF está completo, para não punir durante a digitação.
const cpfInvalid = computed(
  () => cpf.value.replace(/\D/g, '').length === 11 && !isValidCpf(cpf.value),
)

const passwordStrength = computed(() => {
  const p = password.value
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (p.length >= 10 && /[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/\d/.test(p) && /[^A-Za-z0-9]/.test(p)) score++
  return score
})

const strengthMeta = [
  { label: '', color: '' },
  { label: 'Senha fraca', color: 'bg-brand-red' },
  { label: 'Senha média', color: 'bg-amber-500' },
  { label: 'Senha forte', color: 'bg-emerald-600' },
] as const

function maskCpf(value: string) {
  cpf.value = value
    .replace(/\D/g, '')
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2')
}

function maskPhone(value: string) {
  const d = value.replace(/\D/g, '').slice(0, 11)
  const masked = d.replace(/^(\d{2})(\d)/, '($1) $2')
  phone.value =
    d.length > 10
      ? masked.replace(/(\d{5})(\d{4})$/, '$1-$2')
      : masked.replace(/(\d{4})(\d{1,4})$/, '$1-$2')
}

async function onSubmit() {
  error.value = null
  if (emailMismatch.value) {
    error.value = 'Os e-mails informados não coincidem.'
    return
  }
  if (cpf.value && !isValidCpf(cpf.value)) {
    error.value = 'O CPF informado não é válido.'
    return
  }
  if (phone.value && phone.value.replace(/\D/g, '').length < 10) {
    error.value = 'Informe o telefone com DDD.'
    return
  }
  if (password.value.length < 8) {
    error.value = 'A senha deve ter pelo menos 8 caracteres.'
    return
  }
  if (passwordMismatch.value) {
    error.value = 'As senhas informadas não coincidem.'
    return
  }
  loading.value = true
  try {
    await auth.register({
      name: name.value,
      email: email.value,
      password: password.value,
    })
    router.push('/')
  } catch (e) {
    if (axios.isAxiosError(e) && e.response?.status === 409) {
      error.value = 'Já existe uma conta com este e-mail.'
    } else if (axios.isAxiosError(e) && e.response?.status === 400) {
      error.value = 'Dados inválidos. Revise os campos e tente novamente.'
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
    <form class="flex h-full flex-col gap-4" @submit.prevent="onSubmit">
      <!-- Dados de acesso -->
      <p class="border-b border-brand-navy/10 pb-1.5 text-[11px] font-semibold tracking-widest text-brand-navy/50 uppercase">
        Dados de acesso
      </p>

      <div class="space-y-1.5">
        <Label for="email">E-mail</Label>
        <Input
          id="email"
          v-model="email"
          type="email"
          placeholder="seu@email.com"
          autocomplete="email"
          autofocus
          required
        />
      </div>

      <div class="space-y-1.5">
        <Label for="email-confirm">Repita seu e-mail</Label>
        <Input
          id="email-confirm"
          v-model="emailConfirm"
          type="email"
          placeholder="seu@email.com"
          autocomplete="email"
          :aria-invalid="emailMismatch || undefined"
          required
        />
        <p v-if="emailMismatch" class="text-xs text-destructive">Os e-mails não coincidem.</p>
      </div>

      <!-- Dados pessoais e profissionais -->
      <p class="mt-2 border-b border-brand-navy/10 pb-1.5 text-[11px] font-semibold tracking-widest text-brand-navy/50 uppercase">
        Dados do veterinário
      </p>

      <div class="space-y-1.5">
        <Label for="name">Nome completo</Label>
        <Input
          id="name"
          v-model="name"
          placeholder="Digite seu nome completo"
          autocomplete="name"
          maxlength="150"
          required
        />
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div class="space-y-1.5">
          <Label for="cpf">CPF <span class="font-normal text-brand-navy/40 normal-case">(opcional)</span></Label>
          <Input
            id="cpf"
            :model-value="cpf"
            inputmode="numeric"
            placeholder="000.000.000-00"
            :aria-invalid="cpfInvalid || undefined"
            @update:model-value="maskCpf(String($event))"
          />
          <p v-if="cpfInvalid" class="text-xs text-destructive">CPF inválido.</p>
        </div>
        <div class="space-y-1.5">
          <Label for="phone">Telefone</Label>
          <Input
            id="phone"
            :model-value="phone"
            inputmode="numeric"
            placeholder="(00) 90000-0000"
            autocomplete="tel-national"
            @update:model-value="maskPhone(String($event))"
          />
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div class="space-y-1.5">
          <Label for="crmv">CRMV</Label>
          <Input id="crmv" v-model="crmv" placeholder="CRMV-UF 00000" />
        </div>
        <div class="space-y-1.5">
          <Label for="specialty">Especialidade</Label>
          <Input id="specialty" v-model="specialty" placeholder="Cardiologia" />
        </div>
      </div>

      <!-- Senha -->
      <p class="mt-2 border-b border-brand-navy/10 pb-1.5 text-[11px] font-semibold tracking-widest text-brand-navy/50 uppercase">
        Senha
      </p>

      <div class="space-y-1.5">
        <Label for="password">Senha</Label>
        <PasswordInput
          id="password"
          v-model="password"
          placeholder="Mínimo de 8 caracteres"
          autocomplete="new-password"
          minlength="8"
          required
        />
        <div v-if="password" class="flex items-center gap-2 pt-1" aria-live="polite">
          <div class="flex flex-1 gap-1">
            <span
              v-for="i in 3"
              :key="i"
              class="h-1 flex-1 rounded-full transition-colors"
              :class="i <= passwordStrength ? strengthMeta[passwordStrength].color : 'bg-brand-navy/10'"
            />
          </div>
          <span class="text-[11px] font-medium text-brand-navy/60">
            {{ strengthMeta[passwordStrength].label }}
          </span>
        </div>
      </div>

      <div class="space-y-1.5">
        <Label for="password-confirm">Repetir senha</Label>
        <PasswordInput
          id="password-confirm"
          v-model="passwordConfirm"
          placeholder="Repita a senha"
          autocomplete="new-password"
          :aria-invalid="passwordMismatch || undefined"
          required
        />
        <p v-if="passwordMismatch" class="text-xs text-destructive">As senhas não coincidem.</p>
      </div>

      <p
        v-if="error"
        role="alert"
        class="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive"
      >
        {{ error }}
      </p>

      <div class="mt-auto pt-4 text-center">
        <Button
          type="submit"
          size="lg"
          class="w-full max-w-[250px] tracking-widest uppercase"
          :disabled="loading"
        >
          <Loader2 v-if="loading" class="animate-spin" />
          {{ loading ? 'Cadastrando...' : 'Cadastrar' }}
        </Button>
        <p class="mt-3 text-[11px] leading-relaxed text-brand-navy/50">
          Já tem uma conta?
          <RouterLink to="/login" class="font-semibold text-brand-red underline-offset-4 hover:underline">
            Faça login
          </RouterLink>
        </p>
      </div>
    </form>
  </AuthShell>
</template>

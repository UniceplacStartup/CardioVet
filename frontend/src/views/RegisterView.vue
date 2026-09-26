<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2 } from 'lucide-vue-next'
import axios from 'axios'
import AuthShell from '@/components/auth/AuthShell.vue'
import { Button } from '@/components/ui/button'
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
const showPassword = ref(false)
</script>

<template>
  <AuthShell>
    <form class="flex flex-col gap-2.5" @submit.prevent="onSubmit">
      <!-- E-MAIL * -->
      <div class="space-y-1">
        <label for="email" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
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
          class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] placeholder:uppercase outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
        />
      </div>

      <!-- CONFIRME SEU E-MAIL * -->
      <div class="space-y-1">
        <label for="email-confirm" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
          CONFIRME SEU E-MAIL <span class="text-brand-red">*</span>
        </label>
        <input
          id="email-confirm"
          v-model="emailConfirm"
          type="email"
          placeholder="SEU@EMAIL.COM"
          autocomplete="email"
          required
          class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] placeholder:uppercase outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
        />
        <p v-if="emailMismatch" class="text-[10px] font-medium text-destructive">Os e-mails não coincidem.</p>
      </div>

      <!-- NOME * -->
      <div class="space-y-1">
        <label for="name" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
          NOME <span class="text-brand-red">*</span>
        </label>
        <input
          id="name"
          v-model="name"
          type="text"
          placeholder="DIGITE SEU NOME COMPLETO"
          autocomplete="name"
          maxlength="150"
          required
          class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] placeholder:uppercase outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
        />
      </div>

      <!-- CPF * & TELEFONE * -->
      <div class="grid grid-cols-2 gap-2.5">
        <div class="space-y-1">
          <label for="cpf" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
            CPF <span class="text-brand-red">*</span>
          </label>
          <input
            id="cpf"
            :value="cpf"
            inputmode="numeric"
            placeholder="000.000.000-00"
            required
            class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
            @input="maskCpf(($event.target as HTMLInputElement).value)"
          />
          <p v-if="cpfInvalid" class="text-[10px] font-medium text-destructive">CPF inválido.</p>
        </div>
        <div class="space-y-1">
          <label for="phone" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
            TELEFONE <span class="text-brand-red">*</span>
          </label>
          <input
            id="phone"
            :value="phone"
            inputmode="numeric"
            placeholder="(00) 00000-0000"
            autocomplete="tel-national"
            required
            class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
            @input="maskPhone(($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>

      <!-- CRMV * & ESPECIALIDADE * -->
      <div class="grid grid-cols-2 gap-2.5">
        <div class="space-y-1">
          <label for="crmv" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
            CRMV <span class="text-brand-red">*</span>
          </label>
          <input
            id="crmv"
            v-model="crmv"
            type="text"
            placeholder="CRMV-UF 00000"
            required
            class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] placeholder:uppercase outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
          />
        </div>
        <div class="space-y-1">
          <label for="specialty" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
            ESPECIALIDADE <span class="text-brand-red">*</span>
          </label>
          <input
            id="specialty"
            v-model="specialty"
            type="text"
            placeholder="CARDIOLOGIA"
            required
            class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] placeholder:uppercase outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
          />
        </div>
      </div>

      <!-- SENHA * -->
      <div class="space-y-1">
        <label for="password" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
          SENHA <span class="text-brand-red">*</span>
        </label>
        <input
          id="password"
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="••••••••••••••••"
          autocomplete="new-password"
          required
          class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
        />
      </div>

      <!-- REPETIR SENHA * -->
      <div class="space-y-1">
        <label for="password-confirm" class="block text-[11px] font-semibold tracking-wide text-brand-navy uppercase select-none">
          REPETIR SENHA <span class="text-brand-red">*</span>
        </label>
        <input
          id="password-confirm"
          v-model="passwordConfirm"
          :type="showPassword ? 'text' : 'password'"
          placeholder="••••••••••••••••"
          autocomplete="new-password"
          required
          class="h-[36px] w-full rounded-[10px] bg-[#D6DBE1] px-3.5 text-xs font-medium text-brand-navy placeholder:text-[#8D98A7] outline-none transition-all focus:bg-[#E2E6EC] focus:ring-1 focus:ring-brand-red/50"
        />
        <p v-if="passwordMismatch" class="text-[10px] font-medium text-destructive">As senhas não coincidem.</p>
      </div>

      <!-- MOSTRAR SENHA & AVISO -->
      <div class="flex items-center justify-between pt-0.5 text-[10px] select-none">
        <label class="flex cursor-pointer items-center gap-1.5 text-brand-navy/80 hover:text-brand-navy">
          <input
            v-model="showPassword"
            type="checkbox"
            class="size-3.5 rounded border-gray-300 text-brand-red accent-brand-red cursor-pointer"
          />
          <span class="font-semibold uppercase tracking-wider text-[9px]">MOSTRAR SENHA</span>
        </label>

        <span class="text-[8px] font-semibold uppercase tracking-wider text-brand-navy/60">
          <span class="text-brand-red">*</span> ESPAÇOS COM PREENCHIMENTO OBRIGATÓRIO
        </span>
      </div>

      <!-- Regras de validação de senha -->
      <div class="rounded-[8px] bg-[#DDE2E8] p-2.5 text-[9.5px] leading-relaxed text-[#4A5568] select-none">
        <p>A senha deve ter no mínimo 8 caracteres.</p>
        <p>A senha deve conter pelo menos 1 letra maiúscula (A–Z).</p>
        <p>A senha deve conter pelo menos 1 letra minúscula (a–z).</p>
        <p>A senha deve conter pelo menos 1 número (0–9).</p>
        <p>A senha deve conter pelo menos 1 caractere especial (ex: @, #, $, %, &, *).</p>
      </div>

      <!-- Erro geral -->
      <p
        v-if="error"
        role="alert"
        class="rounded-lg bg-destructive/10 px-3 py-1.5 text-xs font-medium text-destructive"
      >
        {{ error }}
      </p>

      <!-- Botão CADASTRAR -->
      <div class="pt-2">
        <Button
          type="submit"
          class="h-[41px] w-full rounded-[10px] bg-brand-red text-sm font-bold tracking-widest text-white uppercase shadow-sm transition-all hover:bg-[#8F1818] active:scale-[0.99] disabled:opacity-70"
          :disabled="loading"
        >
          <Loader2 v-if="loading" class="size-4 animate-spin" />
          {{ loading ? 'CADASTRANDO...' : 'CADASTRAR' }}
        </Button>
      </div>
    </form>
  </AuthShell>
</template>

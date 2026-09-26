<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Settings, Lock, LogOut, ChevronRight, ChevronLeft, Camera, Edit3, Check } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import perfImg from '@/assets/perf.png'

const auth = useAuthStore()
const router = useRouter()

// Controla visualização entre Perfil 1 (Visão Geral) e Perfil 2 (Edição)
const isEditing = ref(false)

// Modal de redefinir senha
const showPasswordModal = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordSuccess = ref(false)

// Dados do formulário de edição
const formData = ref({
  fullName: auth.user?.name || 'Aline Rosa',
  cpf: '123.456.789-00',
  email: auth.user?.email || 'admin@email.com',
  phone: '(61) 99123-4567',
  crmv: 'CRMV-DF 4521',
  specialty: 'Cardiologia Veterinária',
})

const saveSuccessMessage = ref(false)

function handleLogout() {
  auth.logout()
  router.push({ name: 'login' })
}

function handleSaveProfile() {
  saveSuccessMessage.value = true
  setTimeout(() => {
    saveSuccessMessage.value = false
    isEditing.value = false
  }, 1200)
}

function handleResetPassword() {
  if (newPassword.value && newPassword.value === confirmPassword.value) {
    passwordSuccess.value = true
    setTimeout(() => {
      passwordSuccess.value = false
      showPasswordModal.value = false
      oldPassword.value = ''
      newPassword.value = ''
      confirmPassword.value = ''
    }, 1500)
  }
}
</script>

<template>
  <div class="space-y-4 select-none">
    <!-- VISÃO GERAL DO PERFIL -->
    <template v-if="!isEditing">
      <h1 class="text-3xl font-bold tracking-tight text-white">
        Meu perfil
      </h1>

      <div class="rounded-[18px] bg-[#D9DDE2] p-6 sm:p-10 shadow-xl flex flex-col items-center">
        <div class="w-full max-w-[480px] rounded-[24px] bg-white p-7 sm:p-8 shadow-sm flex flex-col items-center">
          <!-- Foto de perfil -->
          <div class="relative">
            <img
              :src="perfImg"
              alt="Dra. Aline Rosa"
              class="size-28 sm:size-32 rounded-full object-cover ring-4 ring-[#16273e] shadow-sm"
            />
            <button
              type="button"
              title="Alterar foto"
              class="absolute bottom-0 right-0 flex size-8 items-center justify-center rounded-full bg-[#0284c7] text-white ring-2 ring-white transition-transform hover:scale-105 active:scale-95 cursor-pointer shadow-sm"
              @click="isEditing = true"
            >
              <Camera class="size-4" />
            </button>
          </div>

          <!-- Nome e especialidade -->
          <h2 class="mt-3 text-lg font-bold text-[#0F2440]">
            Dra. Aline Rosa
          </h2>
          <span class="mt-1 inline-flex items-center justify-center rounded-full bg-[#FFEDED] px-2.5 py-0.5 text-[10px] font-semibold text-brand-red">
            Cardiologia Veterinária
          </span>

          <!-- Opções de navegação do perfil -->
          <div class="mt-6 w-full max-w-[344px] space-y-3">
            <!-- EDITAR PERFIL -->
            <button
              type="button"
              class="flex w-full items-center justify-between rounded-[16px] border border-slate-400/60 bg-white px-4 py-3 text-left transition-all hover:border-[#14253B] hover:bg-slate-50/80 hover:shadow-xs cursor-pointer"
              @click="isEditing = true"
            >
              <div class="flex items-center gap-3">
                <div class="text-[#14253B]">
                  <Settings class="size-5" />
                </div>
                <div>
                  <p class="text-xs font-bold tracking-wider text-[#14253B] uppercase leading-tight">
                    EDITAR PERFIL
                  </p>
                  <p class="text-[10px] text-slate-500 leading-tight mt-0.5">
                    Edite as informações do seu perfil
                  </p>
                </div>
              </div>
              <ChevronRight class="size-4 text-[#14253B]" />
            </button>

            <!-- REDEFINIR SENHA -->
            <button
              type="button"
              class="flex w-full items-center justify-between rounded-[16px] border border-slate-400/60 bg-white px-4 py-3 text-left transition-all hover:border-[#14253B] hover:bg-slate-50/80 hover:shadow-xs cursor-pointer"
              @click="showPasswordModal = true"
            >
              <div class="flex items-center gap-3">
                <div class="text-[#14253B]">
                  <Lock class="size-5" />
                </div>
                <div>
                  <p class="text-xs font-bold tracking-wider text-[#14253B] uppercase leading-tight">
                    REDEFINIR SENHA
                  </p>
                  <p class="text-[10px] text-slate-500 leading-tight mt-0.5">
                    Altere sua senha de acesso
                  </p>
                </div>
              </div>
              <ChevronRight class="size-4 text-[#14253B]" />
            </button>

            <!-- SAIR DA CONTA -->
            <button
              type="button"
              class="flex w-full items-center justify-between rounded-[16px] border border-slate-400/60 bg-white px-4 py-3.5 text-left transition-all hover:border-brand-red/50 hover:bg-red-50/40 hover:shadow-xs cursor-pointer"
              @click="handleLogout"
            >
              <div class="flex items-center gap-3">
                <div class="text-brand-red">
                  <LogOut class="size-5 stroke-[2.5]" />
                </div>
                <div>
                  <p class="text-xs font-bold tracking-wider text-brand-red uppercase leading-tight">
                    SAIR DA CONTA
                  </p>
                </div>
              </div>
              <ChevronRight class="size-4 text-[#14253B]" />
            </button>
          </div>

          <!-- Botão Cancelar -->
          <div class="mt-7">
            <button
              type="button"
              class="rounded-[10px] border border-slate-300 bg-white px-7 py-1.5 text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-800 cursor-pointer shadow-2xs"
              @click="router.push('/pacientes')"
            >
              Cancelar
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- EDIÇÃO DE DADOS DO PERFIL -->
    <template v-else>
      <!-- Voltar para Configurações -->
      <button
        type="button"
        class="inline-flex items-center gap-1.5 text-xs font-medium text-slate-300 hover:text-white transition-colors cursor-pointer"
        @click="isEditing = false"
      >
        <ChevronLeft class="size-4" />
        <span>Voltar para Configurações</span>
      </button>

      <h1 class="text-3xl font-bold tracking-tight text-white">
        Meu perfil
      </h1>

      <div class="rounded-[18px] bg-[#D9DDE2] p-6 sm:p-8 shadow-xl">
        <div class="rounded-2xl bg-white p-6 sm:p-8 shadow-sm space-y-6">
          <!-- Cabeçalho com mini avatar e identificação -->
          <div class="flex items-center gap-4 border-b border-slate-100 pb-5">
            <img
              :src="perfImg"
              alt="Dra. Aline Rosa"
              class="size-14 rounded-full object-cover ring-2 ring-slate-200 shadow-xs"
            />
            <div>
              <h2 class="text-lg font-bold text-[#0F2440]">
                Dra. Aline Rosa
              </h2>
              <span class="mt-1 inline-flex items-center justify-center rounded-full bg-[#FFEDED] px-2.5 py-0.5 text-[10px] font-semibold text-brand-red">
                Cardiologia Veterinária
              </span>
            </div>
          </div>

          <!-- Alerta de sucesso se salvo -->
          <div
            v-if="saveSuccessMessage"
            class="flex items-center gap-2 rounded-xl bg-emerald-50 border border-emerald-300 p-3 text-xs font-semibold text-emerald-800"
          >
            <Check class="size-4" />
            <span>Perfil atualizado com sucesso!</span>
          </div>

          <!-- Formulário de dados -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-5">
            <!-- Nome Completo -->
            <div class="rounded-xl border border-slate-400/80 bg-white p-3 transition-all focus-within:border-[#14253B] focus-within:ring-1 focus-within:ring-[#14253B]">
              <label class="block text-[11px] font-medium text-slate-500">Nome completo</label>
              <input
                v-model="formData.fullName"
                type="text"
                class="w-full text-sm font-semibold text-[#14253B] bg-transparent outline-none pt-0.5"
              />
            </div>

            <!-- CPF -->
            <div class="rounded-xl border border-slate-400/80 bg-white p-3 transition-all focus-within:border-[#14253B] focus-within:ring-1 focus-within:ring-[#14253B]">
              <label class="block text-[11px] font-medium text-slate-500">CPF</label>
              <input
                v-model="formData.cpf"
                type="text"
                class="w-full text-sm font-semibold text-[#14253B] bg-transparent outline-none pt-0.5"
              />
            </div>

            <!-- E-mail -->
            <div class="rounded-xl border border-slate-400/80 bg-white p-3 transition-all focus-within:border-[#14253B] focus-within:ring-1 focus-within:ring-[#14253B]">
              <label class="block text-[11px] font-medium text-slate-500">E-mail</label>
              <input
                v-model="formData.email"
                type="email"
                class="w-full text-sm font-semibold text-[#14253B] bg-transparent outline-none pt-0.5"
              />
            </div>

            <!-- Telefone -->
            <div class="rounded-xl border border-slate-400/80 bg-white p-3 transition-all focus-within:border-[#14253B] focus-within:ring-1 focus-within:ring-[#14253B]">
              <label class="block text-[11px] font-medium text-slate-500">Telefone</label>
              <input
                v-model="formData.phone"
                type="text"
                class="w-full text-sm font-semibold text-[#14253B] bg-transparent outline-none pt-0.5"
              />
            </div>

            <!-- CRMV -->
            <div class="rounded-xl border border-slate-400/80 bg-white p-3 transition-all focus-within:border-[#14253B] focus-within:ring-1 focus-within:ring-[#14253B]">
              <label class="block text-[11px] font-medium text-slate-500">CRMV</label>
              <input
                v-model="formData.crmv"
                type="text"
                class="w-full text-sm font-semibold text-[#14253B] bg-transparent outline-none pt-0.5"
              />
            </div>

            <!-- Especialidade -->
            <div class="rounded-xl border border-slate-400/80 bg-white p-3 transition-all focus-within:border-[#14253B] focus-within:ring-1 focus-within:ring-[#14253B]">
              <label class="block text-[11px] font-medium text-slate-500">Especialidade</label>
              <input
                v-model="formData.specialty"
                type="text"
                class="w-full text-sm font-semibold text-[#14253B] bg-transparent outline-none pt-0.5"
              />
            </div>
          </div>

          <!-- Barra de Ações Inferior -->
          <div class="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-slate-100">
            <!-- Botão Alterar senha à esquerda -->
            <button
              type="button"
              class="rounded-lg bg-brand-red px-6 py-2.5 text-xs font-bold text-white transition-colors hover:bg-[#8e1818] cursor-pointer shadow-xs"
              @click="showPasswordModal = true"
            >
              Alterar senha
            </button>

            <!-- Botões Cancelar e Salvar alterações à direita -->
            <div class="flex items-center gap-3">
              <button
                type="button"
                class="rounded-lg border border-slate-300 bg-white px-6 py-2.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-50 cursor-pointer"
                @click="isEditing = false"
              >
                Cancelar
              </button>
              <button
                type="button"
                class="rounded-lg bg-brand-red px-6 py-2.5 text-xs font-bold text-white transition-colors hover:bg-[#8e1818] cursor-pointer shadow-xs"
                @click="handleSaveProfile"
              >
                Salvar alterações
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ========================================== -->
    <!-- MODAL DE REDEFINIÇÃO DE SENHA             -->
    <!-- ========================================== -->
    <div
      v-if="showPasswordModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4"
    >
      <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl space-y-4">
        <h3 class="text-lg font-bold text-[#14253B]">
          Redefinir Senha
        </h3>
        <p class="text-xs text-slate-500">
          Informe sua senha atual e escolha uma nova senha de acesso.
        </p>

        <div v-if="passwordSuccess" class="rounded-xl bg-emerald-50 border border-emerald-300 p-3 text-xs font-semibold text-emerald-800">
          Senha alterada com sucesso!
        </div>

        <div class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-slate-600 mb-1">Senha atual</label>
            <input
              v-model="oldPassword"
              type="password"
              placeholder="Digite sua senha atual"
              class="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-[#14253B]"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-600 mb-1">Nova senha</label>
            <input
              v-model="newPassword"
              type="password"
              placeholder="Digite a nova senha"
              class="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-[#14253B]"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-600 mb-1">Confirmar nova senha</label>
            <input
              v-model="confirmPassword"
              type="password"
              placeholder="Confirme a nova senha"
              class="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-[#14253B]"
            />
          </div>
        </div>

        <div class="flex items-center justify-end gap-3 pt-3">
          <button
            type="button"
            class="rounded-lg border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer"
            @click="showPasswordModal = false"
          >
            Fechar
          </button>
          <button
            type="button"
            class="rounded-lg bg-brand-red px-5 py-2 text-xs font-bold text-white hover:bg-[#8e1818] cursor-pointer shadow-xs"
            @click="handleResetPassword"
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

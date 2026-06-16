<script setup lang="ts">
import heroImg from '@/assets/auth-hero.jpg'
import logoFull from '@/assets/logo-full.png'
import logoMark from '@/assets/logo-mark.png'
</script>

<template>
  <div class="flex min-h-svh items-center justify-center bg-brand-navy p-4 sm:p-8">
    <div
      class="auth-card flex w-full max-w-6xl overflow-hidden rounded-[10px] bg-brand-surface shadow-2xl shadow-black/40"
    >
      <!-- Painel da foto (oculto em telas pequenas) -->
      <div class="relative hidden flex-1 bg-white lg:block">
        <img
          :src="heroImg"
          alt="Cão e gato lado a lado"
          class="absolute inset-0 size-full object-cover object-top"
        />
        <img
          :src="logoMark"
          alt=""
          class="absolute bottom-6 left-6 w-28 drop-shadow-sm"
        />
      </div>

      <!-- Painel do formulário -->
      <div class="flex w-full flex-col px-8 py-8 sm:px-10 lg:max-w-[420px]">
        <img
          :src="logoFull"
          alt="CardioVet — Laudos cardiológicos"
          class="auth-reveal mx-auto w-64"
        />

        <!-- Linha de batimento cardíaco -->
        <svg
          class="auth-reveal mx-auto mt-4 text-brand-red/70"
          style="animation-delay: 80ms"
          width="220"
          height="18"
          viewBox="0 0 220 18"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="M0 9h70l8-6 8 12 8-14 8 14 6-6h112"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linejoin="round"
          />
        </svg>

        <!-- Abas Login / Cadastrar -->
        <nav class="auth-reveal mt-6 grid grid-cols-2 gap-2" style="animation-delay: 140ms">
          <RouterLink
            to="/login"
            class="flex h-10 items-center justify-center rounded-[10px] text-sm font-semibold tracking-widest uppercase transition-colors"
            :class="$route.name === 'login'
              ? 'bg-brand-red text-white shadow-sm'
              : 'bg-brand-navy/10 text-brand-navy hover:bg-brand-navy/15'"
          >
            Login
          </RouterLink>
          <RouterLink
            to="/cadastro"
            class="flex h-10 items-center justify-center rounded-[10px] text-sm font-semibold tracking-widest uppercase transition-colors"
            :class="$route.name === 'register'
              ? 'bg-brand-red text-white shadow-sm'
              : 'bg-brand-navy/10 text-brand-navy hover:bg-brand-navy/15'"
          >
            Cadastrar
          </RouterLink>
        </nav>

        <div class="auth-reveal mt-8 flex-1" style="animation-delay: 200ms">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  animation: auth-card-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.auth-reveal {
  animation: auth-reveal-in 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes auth-card-in {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.99);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes auth-reveal-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-card,
  .auth-reveal {
    animation: none;
  }
}
</style>

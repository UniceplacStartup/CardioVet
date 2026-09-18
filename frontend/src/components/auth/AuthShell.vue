<script setup lang="ts">
import { ArrowLeft } from 'lucide-vue-next'
import heroImg from '@/assets/auth-hero.jpg'
import logoFull from '@/assets/logo-full.png'
</script>

<template>
  <div class="relative flex min-h-svh w-full items-center justify-center overflow-x-hidden bg-[#24354a] p-4 sm:p-8 lg:justify-end lg:pr-16 xl:pr-24">
    <RouterLink
      v-if="$route.name === 'register'"
      to="/login"
      class="absolute top-6 left-6 z-30 flex size-11 items-center justify-center rounded-full bg-brand-red text-white shadow-lg transition-transform hover:scale-105 active:scale-95 sm:top-8 sm:left-8"
      title="Voltar para o Login"
      aria-label="Voltar para o Login"
    >
      <ArrowLeft class="size-6 stroke-[2.5]" />
    </RouterLink>

    <div class="pointer-events-none absolute inset-0 z-0 overflow-hidden bg-[#26374a]" aria-hidden="true">
      <img
        :src="heroImg"
        alt=""
        class="h-full w-full object-cover object-[20%_top] sm:object-[28%_top] lg:object-[32%_center] mix-blend-multiply opacity-70 brightness-[0.72] contrast-[1.08] saturate-[0.85]"
      />
      <div class="absolute inset-0 bg-[#162536]/40 mix-blend-multiply"></div>
    </div>

    <div
      class="auth-card relative z-10 w-full max-w-[420px] rounded-[16px] bg-white p-7 shadow-2xl shadow-black/40 sm:p-9 max-h-[calc(100svh-4rem)] overflow-y-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden"
    >
      <div class="flex justify-center pb-5 pt-1">
        <img
          :src="logoFull"
          alt="CardioVet — Laudos cardiológicos"
          class="w-60 max-w-full select-none"
        />
      </div>

      <nav class="grid grid-cols-2 gap-3 pb-5">
        <RouterLink
          to="/login"
          class="flex h-[41px] items-center justify-center rounded-[10px] text-sm font-semibold tracking-wider uppercase transition-colors"
          :class="$route.name === 'login'
            ? 'bg-brand-red text-white shadow-sm'
            : 'bg-[#D6DBE1] text-[#4A5568] hover:bg-[#CAD0D8]'"
        >
          Login
        </RouterLink>
        <RouterLink
          to="/cadastro"
          class="flex h-[41px] items-center justify-center rounded-[10px] text-sm font-semibold tracking-wider uppercase transition-colors"
          :class="$route.name === 'register'
            ? 'bg-brand-red text-white shadow-sm'
            : 'bg-[#D6DBE1] text-[#4A5568] hover:bg-[#CAD0D8]'"
        >
          Cadastrar
        </RouterLink>
      </nav>

      <slot />
    </div>

    <footer class="pointer-events-none absolute inset-x-0 bottom-3 z-10 text-center text-xs font-normal tracking-wide text-white/80 select-none drop-shadow-sm">
      © 2026 CardioVet. Todos os direitos reservados.
    </footer>
  </div>
</template>

<style scoped>
.auth-card {
  animation: auth-card-in 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes auth-card-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-card {
    animation: none;
  }
}
</style>

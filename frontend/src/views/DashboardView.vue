<script setup lang="ts">
import { PawPrint, Activity, FileText } from 'lucide-vue-next'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const cards = [
  { title: 'Pacientes', value: '—', description: 'cadastrados', icon: PawPrint },
  { title: 'Exames', value: '—', description: 'realizados', icon: Activity },
  { title: 'Laudos', value: '—', description: 'gerados', icon: FileText },
]
</script>

<template>
  <div class="space-y-8">
    <div>
      <h1 class="text-2xl font-semibold tracking-tight">
        Olá, {{ auth.user?.name }}
      </h1>
      <p class="text-muted-foreground">Visão geral da clínica</p>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <Card v-for="card in cards" :key="card.title">
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium">{{ card.title }}</CardTitle>
          <component :is="card.icon" class="size-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{{ card.value }}</div>
          <CardDescription>{{ card.description }}</CardDescription>
        </CardContent>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/lib/api'
import type { Page, Patient } from '@/types'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'

const patients = ref<Patient[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')

async function load() {
  loading.value = true
  error.value = null
  try {
    const { data } = await api.get<Page<Patient>>('/patients', {
      params: { search: search.value || undefined, size: 50 },
    })
    patients.value = data.content
  } catch {
    error.value = 'Não foi possível carregar os pacientes.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Pacientes</h1>
        <p class="text-muted-foreground">Animais cadastrados na clínica</p>
      </div>
    </div>

    <Input
      v-model="search"
      placeholder="Buscar por nome..."
      class="max-w-sm"
      @keyup.enter="load"
    />

    <Card>
      <CardContent class="p-0">
        <table class="w-full text-sm">
          <thead class="border-b text-left text-muted-foreground">
            <tr>
              <th class="px-4 py-3 font-medium">Nome</th>
              <th class="px-4 py-3 font-medium">Espécie</th>
              <th class="px-4 py-3 font-medium">Raça</th>
              <th class="px-4 py-3 font-medium">Tutor</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td class="px-4 py-6 text-center text-muted-foreground" colspan="4">
                Carregando...
              </td>
            </tr>
            <tr v-else-if="error">
              <td class="px-4 py-6 text-center text-destructive" colspan="4">{{ error }}</td>
            </tr>
            <tr v-else-if="patients.length === 0">
              <td class="px-4 py-6 text-center text-muted-foreground" colspan="4">
                Nenhum paciente cadastrado.
              </td>
            </tr>
            <tr
              v-for="patient in patients"
              v-else
              :key="patient.id"
              class="border-b last:border-0 hover:bg-muted/50"
            >
              <td class="px-4 py-3 font-medium">{{ patient.name }}</td>
              <td class="px-4 py-3">{{ patient.species }}</td>
              <td class="px-4 py-3">{{ patient.breed ?? '—' }}</td>
              <td class="px-4 py-3">{{ patient.tutorName }}</td>
            </tr>
          </tbody>
        </table>
      </CardContent>
    </Card>
  </div>
</template>

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api, { getToken, setToken } from '@/lib/api'
import type { AuthResponse, LoginRequest, RegisterRequest, Role } from '@/types'

const USER_KEY = 'cardiovet.user'

interface SessionUser {
  userId: string
  name: string
  email: string
  role: Role
}

function loadUser(): SessionUser | null {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? (JSON.parse(raw) as SessionUser) : null
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<SessionUser | null>(loadUser())
  const token = ref<string | null>(getToken())

  const isAuthenticated = computed(() => !!token.value)

  function persist(data: AuthResponse) {
    const session: SessionUser = {
      userId: data.userId,
      name: data.name,
      email: data.email,
      role: data.role,
    }
    user.value = session
    token.value = data.token
    setToken(data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(session))
  }

  async function login(payload: LoginRequest) {
    const { data } = await api.post<AuthResponse>('/auth/login', payload)
    persist(data)
  }

  async function register(payload: RegisterRequest) {
    const { data } = await api.post<AuthResponse>('/auth/register', payload)
    persist(data)
  }

  function logout() {
    user.value = null
    token.value = null
    setToken(null)
    localStorage.removeItem(USER_KEY)
  }

  return { user, token, isAuthenticated, login, register, logout }
})

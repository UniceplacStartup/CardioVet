import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import api, { getToken, isTokenValid, redirectToLogin, setToken, tokenExpiresAt } from '@/lib/api'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
  UserProfile,
} from '@/types'

const USER_KEY = 'cardiovet.user'

function loadUser(): UserProfile | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserProfile
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

function initialToken(): string | null {
  const stored = getToken()
  if (isTokenValid(stored)) return stored
  setToken(null)
  localStorage.removeItem(USER_KEY)
  return null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(initialToken())
  const user = ref<UserProfile | null>(token.value ? loadUser() : null)
  let expiryTimer: ReturnType<typeof setTimeout> | undefined

  const isAuthenticated = computed(() => !!token.value)

  function scheduleExpiry() {
    clearTimeout(expiryTimer)
    const exp = token.value ? tokenExpiresAt(token.value) : null
    if (exp === null) return
    expiryTimer = setTimeout(expire, Math.max(exp - Date.now(), 0))
  }

  function expire() {
    clearSession()
    redirectToLogin('expired')
  }

  function isSessionValid(): boolean {
    if (isTokenValid(token.value)) return true
    if (token.value) clearSession()
    return false
  }

  function setUser(profile: UserProfile) {
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
  }

  async function fetchProfile() {
    const { data } = await api.get<UserProfile>('/users/me')
    setUser(data)
    return data
  }

  async function login(payload: LoginRequest) {
    const { data } = await api.post<AuthResponse>('/auth/login', payload)
    token.value = data.token
    setToken(data.token)
    setUser({
      id: data.userId,
      name: data.name,
      email: data.email,
      role: data.role,
      createdAt: '',
    })
    scheduleExpiry()
    await fetchProfile()
  }

  async function register(payload: RegisterRequest) {
    await api.post<UserProfile>('/auth/register', payload)
  }

  async function updateProfile(payload: UpdateProfileRequest) {
    const { data } = await api.put<UserProfile>('/users/me', payload)
    setUser(data)
    return data
  }

  function clearSession() {
    clearTimeout(expiryTimer)
    user.value = null
    token.value = null
    setToken(null)
    localStorage.removeItem(USER_KEY)
  }

  function logout() {
    clearSession()
  }

  scheduleExpiry()

  return {
    user,
    token,
    isAuthenticated,
    isSessionValid,
    login,
    register,
    fetchProfile,
    updateProfile,
    logout,
  }
})

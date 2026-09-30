import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

const TOKEN_KEY = 'cardiovet.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function tokenExpiresAt(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return typeof json.exp === 'number' ? json.exp * 1000 : null
  } catch {
    return null
  }
}

export function isTokenValid(token: string | null): token is string {
  if (!token) return false
  const exp = tokenExpiresAt(token)
  return exp !== null && exp > Date.now()
}

export function redirectToLogin(reason: 'expired') {
  setToken(null)
  localStorage.removeItem('cardiovet.user')
  if (window.location.pathname !== '/login') {
    window.location.assign(`/login?${reason}=1`)
  }
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthCall = String(error.config?.url ?? '').startsWith('/auth/')
    if (error.response?.status === 401 && !isAuthCall) {
      redirectToLogin('expired')
    }
    return Promise.reject(error)
  },
)

export default api

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) return 'Não foi possível conectar ao servidor.'
    const data = error.response.data as { message?: string; fieldErrors?: Record<string, string> } | undefined
    const firstField = data?.fieldErrors ? Object.values(data.fieldErrors)[0] : undefined
    return firstField || data?.message || fallback
  }
  return fallback
}

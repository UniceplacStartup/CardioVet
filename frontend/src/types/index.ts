export type Role = 'ADMIN' | 'VETERINARIO'

export interface AuthResponse {
  token: string
  expiresInMs: number
  userId: string
  name: string
  email: string
  role: Role
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
  role?: Role
}

export interface Patient {
  id: string
  tutorId: string
  tutorName: string
  name: string
  species: string
  breed?: string
  sex?: string
  birthDate?: string
  weightKg?: number
  createdAt: string
}

export type DocumentStatus = 'PENDENTE' | 'PROCESSADO' | 'ERRO'

export interface DocumentField {
  fieldKey: string
  label?: string
  value?: string
  unit?: string
  category?: string
}

export interface DocumentSummary {
  id: string
  fileName: string
  contentType: string
  fileSizeBytes: number
  documentDate?: string
  status: DocumentStatus
  patientId?: string
  patientName?: string
  uploadedById: string
  uploadedByName: string
  fieldCount: number
  createdAt: string
}

export interface DocumentDetail extends Omit<DocumentSummary, 'fieldCount'> {
  errorMessage?: string
  extractedText?: string
  fields: DocumentField[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

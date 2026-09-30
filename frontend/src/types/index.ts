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
  cpf?: string
  phone?: string
  crmv?: string
  specialty?: string
}

export interface UserProfile {
  id: string
  name: string
  email: string
  cpf?: string
  phone?: string
  crmv?: string
  specialty?: string
  role: Role
  createdAt: string
}

export interface UpdateProfileRequest {
  name: string
  cpf?: string
  phone?: string
  crmv?: string
  specialty?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface Tutor {
  id: string
  name: string
  email?: string
  phone?: string
  document?: string
  createdAt: string
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

export interface DocumentImage {
  id: string
  fileName: string
  contentType: string
  fileSizeBytes: number
  createdAt: string
}

export interface DocumentSummary {
  id: string
  fileName: string
  contentType: string
  fileSizeBytes: number
  documentDate?: string
  examDate?: string
  reportModel?: string
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
  species?: string
  breed?: string
  sex?: string
  weightKg?: number
  tutorId?: string
  tutorName?: string
  tutorCpf?: string
  patientAge?: string
  veterinarianName?: string
  findings?: string
  conclusion?: string
  fields: DocumentField[]
  images: DocumentImage[]
}

export interface ReportRequest {
  reportModel?: string
  patientName: string
  species: string
  breed?: string
  sex?: string
  patientAge?: string
  weightKg?: number
  tutorName: string
  tutorCpf?: string
  examDate?: string
  issueDate?: string
  veterinarianName?: string
  findings?: string
  conclusion?: string
}

export interface ApiError {
  status: number
  error: string
  message?: string
  fieldErrors?: Record<string, string>
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

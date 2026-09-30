export interface ReportFormData {
  model: string
  species: string
  age: string
  sex: string
  examDate: string
  patientName: string
  tutorCpf: string
  breed: string
  tutorName: string
  issueDate: string
  weightKg: string
  veterinarian: string
  findings: string
  conclusion: string
}

export const REPORT_MODELS = [
  'Ecocardiograma Transtorácico',
  'Ecocardiograma com Doppler',
  'Eletrocardiograma (ECG)',
  'Holter 24h',
  'Avaliação Pré-operatória',
] as const

export function toIsoDate(value: string): string {
  const match = value.match(/^(\d{2})[/.-](\d{2})[/.-](\d{4})$/)
  if (!match) return ''
  return `${match[3]}-${match[2]}-${match[1]}`
}

export function parseWeight(value: string): number | undefined {
  const match = value.replace(',', '.').match(/\d+(?:\.\d+)?/)
  return match ? Number(match[0]) : undefined
}

export function parseClinicalText(text: string): Partial<ReportFormData> {
  const result: Partial<ReportFormData> = {}

  const findMatch = (regex: RegExp): string => {
    const match = text.match(regex)
    return match && match[1] ? match[1].trim() : ''
  }

  if (/doppler/i.test(text)) {
    result.model = 'Ecocardiograma com Doppler'
  } else if (/eletrocardiograma|ecg/i.test(text)) {
    result.model = 'Eletrocardiograma (ECG)'
  } else if (/holter/i.test(text)) {
    result.model = 'Holter 24h'
  } else if (/pr[ée]-operat[óo]ri/i.test(text)) {
    result.model = 'Avaliação Pré-operatória'
  } else if (/ecocardiograma/i.test(text)) {
    result.model = 'Ecocardiograma Transtorácico'
  }

  const patient = findMatch(/(?:paciente|nome(?:\s+do\s+animal|\s+do\s+paciente)?|animal)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (patient) result.patientName = patient

  const speciesMatch = findMatch(/(?:esp[ée]cie)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (speciesMatch) {
    result.species = speciesMatch
  } else if (/canin[ao]|c[ãa]o/i.test(text)) {
    result.species = 'Canina'
  } else if (/felin[ao]|gato/i.test(text)) {
    result.species = 'Felina'
  }

  const breed = findMatch(/(?:ra[çc]a)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (breed) result.breed = breed

  const age = findMatch(/(?:idade)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (age) result.age = age

  const sexMatch = findMatch(/(?:sexo)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (sexMatch) {
    if (/^m(?:acho)?/i.test(sexMatch)) result.sex = 'Macho'
    else if (/^f(?:[êe]mea)?/i.test(sexMatch)) result.sex = 'Fêmea'
    else result.sex = sexMatch
  } else if (/\bmacho\b/i.test(text)) {
    result.sex = 'Macho'
  } else if (/\bf[êe]mea\b/i.test(text)) {
    result.sex = 'Fêmea'
  }

  const weight = findMatch(/(?:peso(?:\s+do\s+paciente)?)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (weight) result.weightKg = weight

  const tutor = findMatch(/(?:tutor(?:a)?|propriet[áa]rio(?:a)?|respons[áa]vel)\s*[:\-]\s*([^\n\r,;]+)/i)
  if (tutor) result.tutorName = tutor

  const cpf = findMatch(/(?:cpf(?:\s+do\s+tutor)?)\s*[:\-]\s*([\d\.\-]+)/i)
  if (cpf) {
    result.tutorCpf = cpf
  } else {
    const rawCpf = text.match(/\b\d{3}\.\d{3}\.\d{3}\-\d{2}\b/)
    if (rawCpf) result.tutorCpf = rawCpf[0]
  }

  const vet = findMatch(/(?:veterin[áa]ri[oa](?:\s+respons[áa]vel)?|m[ée]dic[oa]\s+veterin[áa]ri[oa])\s*[:\-]\s*([^\n\r,;]+)/i)
  if (vet) {
    result.veterinarian = vet
  } else {
    const drMatch = text.match(/\b(?:Dr[a]?\.\s*[A-ZÀ-Úa-zà-ú]+(?:\s+[A-ZÀ-Úa-zà-ú]+)+)/)
    if (drMatch) result.veterinarian = drMatch[0]
  }

  const examDate = findMatch(/(?:data\s+do\s+exame|data\s+exame)\s*[:\-]\s*(\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4})/i)
  if (examDate) {
    result.examDate = toIsoDate(examDate)
  } else {
    const dates = text.match(/\b\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4}\b/g)
    if (dates && dates.length > 0 && dates[0]) {
      result.examDate = toIsoDate(dates[0])
    }
  }

  const issueDate = findMatch(/(?:data\s+de\s+emiss[ãa]o|data\s+emiss[ãa]o|emitido\s+em)\s*[:\-]\s*(\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4})/i)
  if (issueDate) {
    result.issueDate = toIsoDate(issueDate)
  } else {
    const dates = text.match(/\b\d{2}[\/\-\.]\d{2}[\/\-\.]\d{4}\b/g)
    if (dates && dates.length > 1 && dates[1]) {
      result.issueDate = toIsoDate(dates[1])
    }
  }

  const findingsMatch = text.match(/(?:ACHADOS|DESCRIÇÃO|AVALIAÇÃO)\s*[:\-]?\s*([\s\S]*?)(?=(?:CONCLUSÃO|DIAGNÓSTICO|OBSERVAÇÕES|\n[A-Z\s]{4,}:|$))/i)
  if (findingsMatch && findingsMatch[1]) {
    result.findings = findingsMatch[1].trim()
  }

  const conclusionMatch = text.match(/(?:CONCLUSÃO|DIAGNÓSTICO|IMPRESSÃO DIAGNÓSTICA)\s*[:\-]?\s*([\s\S]*?)(?=(?:Este laudo|Assinado|Dra\.|Dr\.|CRMV|$))/i)
  if (conclusionMatch && conclusionMatch[1]) {
    result.conclusion = conclusionMatch[1].trim()
  }

  return result
}

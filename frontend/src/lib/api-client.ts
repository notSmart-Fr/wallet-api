import { createClient } from '@/lib/supabase/client'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1'

type ApiErrorPayload = {
  status?: number
  error?: string
  message?: string
  timestamp?: string
  traceId?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly traceId?: string

  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.message || `API request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = payload.error || 'UNKNOWN_ERROR'
    this.traceId = payload.traceId
  }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const supabase = createClient()
  const { data: { session } } = await supabase.auth.getSession()

  if (!session) {
    throw new Error('User is not authenticated')
  }

  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${session.access_token}`)
  headers.set('Content-Type', 'application/json')

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as ApiErrorPayload
    throw new ApiError(response.status, error)
  }

  return response.json()
}
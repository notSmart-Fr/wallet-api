import { apiFetch } from '@/lib/api-client'

export type AccountData = {
  id: string
  balance: string
  createdAt: string
}

export function getAccount() {
  return apiFetch<AccountData>('/accounts/me')
}
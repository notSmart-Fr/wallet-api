'use client'

import { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card' // or card primitives
import { apiFetch } from '@/lib/api-client'

interface AccountData {
  accountId: string
  userId: string
  balance: number
  createdAt: string
}

export function BalanceCard() {
  const [account, setAccount] = useState<AccountData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiFetch<AccountData>('/accounts/me')
      .then(setAccount)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="p-4">Loading wallet balance...</div>
  if (error) return <div className="p-4 text-red-500">Error: {error}</div>

  return (
    <div className="rounded-xl border bg-card p-6 text-card-foreground shadow">
      <h3 className="text-sm font-medium text-muted-foreground">Available Balance</h3>
      <div className="mt-2 text-3xl font-bold">
        ${account?.balance.toFixed(2)} USD
      </div>
      <p className="mt-1 text-xs text-muted-foreground">
        Account ID: <code className="bg-muted px-1 py-0.5 rounded">{account?.accountId}</code>
      </p>
    </div>
  )
}
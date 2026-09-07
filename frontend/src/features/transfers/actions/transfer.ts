'use server'

import { createClient } from '@/lib/supabase/server'
import { revalidatePath } from 'next/cache'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1'

export type TransferState = {
  success: boolean
  error?: string
  data?: any
}

export async function executeTransferAction(
  prevState: TransferState,
  formData: FormData
): Promise<TransferState> {
  const recipientAccountId = formData.get('recipientAccountId') as string
  const amount = formData.get('amount') as string

  if (!recipientAccountId || !amount) {
    return { success: false, error: 'Recipient account and amount are required' }
  }

  // Obtain current session token from server cookies
  const supabase = await createClient()
  const { data: { session } } = await supabase.auth.getSession()

  if (!session) {
    return { success: false, error: 'Unauthorized user session' }
  }

  try {
    const res = await fetch(`${API_BASE_URL}/transfers`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${session.access_token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        recipientAccountId,
        amount: parseFloat(amount),
      }),
    })

    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      return { success: false, error: err.message || 'Transfer failed' }
    }

    const data = await res.json()
    revalidatePath('/dashboard')
    return { success: true, data }
  } catch (error: any) {
    return { success: false, error: error.message || 'Internal network error' }
  }
}
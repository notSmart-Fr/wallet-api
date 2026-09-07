'use'

import { useActionState } from 'react'
import { executeTransferAction, type TransferState } from '../actions/transfer'

const initialState: TransferState = {
  success: false,
}

export function TransferForm() {
  const [state, formAction, isPending] = useActionState(executeTransferAction, initialState)

  return (
    <form action={formAction} className="space-y-4 rounded-xl border bg-card p-6 shadow">
      <h3 className="text-lg font-bold">Transfer Money</h3>

      {state.error && (
        <div className="p-3 text-sm text-red-500 bg-red-50 dark:bg-red-950/50 rounded-md">
          {state.error}
        </div>
      )}

      {state.success && (
        <div className="p-3 text-sm text-green-600 bg-green-50 dark:bg-green-950/50 rounded-md">
          Transfer completed successfully!
        </div>
      )}

      <div>
        <label className="block text-xs font-medium text-muted-foreground mb-1">
          Recipient Account UUID
        </label>
        <input
          name="recipientAccountId"
          type="text"
          required
          placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
          className="w-full rounded-md border p-2 text-sm bg-background"
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-muted-foreground mb-1">
          Amount ($)
        </label>
        <input
          name="amount"
          type="number"
          step="0.01"
          min="0.01"
          required
          placeholder="100.00"
          className="w-full rounded-md border p-2 text-sm bg-background"
        />
      </div>

      <button
        type="submit"
        disabled={isPending}
        className="w-full rounded-md bg-primary text-primary-foreground p-2 text-sm font-semibold disabled:opacity-50"
      >
        {isPending ? 'Processing Transfer...' : 'Send Funds'}
      </button>
    </form>
  )
}
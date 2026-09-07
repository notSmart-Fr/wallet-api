import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BalanceCard } from '../BalanceCard'

const { getAccount } = vi.hoisted(() => ({ getAccount: vi.fn() }))

vi.mock('@/features/accounts/api', () => ({ getAccount }))

describe('BalanceCard', () => {
  beforeEach(() => {
    getAccount.mockReset()
  })

  it('shows a stable loading state before the wallet request resolves', () => {
    getAccount.mockReturnValue(new Promise(() => {}))

    render(<BalanceCard />)

    expect(screen.getByText('Loading wallet balance...')).toBeInTheDocument()
  })

  it('renders the account balance returned by the account query', async () => {
    getAccount.mockResolvedValue({
      id: '89d83828-98c7-4ee7-87f7-47ee88063a94',
      balance: '1250.5000',
      createdAt: '2026-09-07T12:00:00Z',
    })

    render(<BalanceCard />)

    expect(await screen.findByText('$1250.50 USD')).toBeInTheDocument()
    expect(screen.getByText('89d83828-98c7-4ee7-87f7-47ee88063a94')).toBeInTheDocument()
  })

  it('shows a user-safe error when the account query fails', async () => {
    getAccount.mockRejectedValue(new Error('Unable to load wallet'))

    render(<BalanceCard />)

    expect(await screen.findByText('Unable to load wallet')).toBeInTheDocument()
  })
})
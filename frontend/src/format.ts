import type { RiskLevel } from './types';

export function formatAmount(amount: number, currency: string): string {
  const crypto = ['BTC', 'ETH'].includes(currency);
  const formatted = amount.toLocaleString('en-GB', {
    minimumFractionDigits: 2,
    maximumFractionDigits: crypto ? 8 : 2,
  });
  return `${formatted} ${currency}`;
}

export function formatDateTime(iso: string | null): string {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

export function riskClass(level: RiskLevel): string {
  return `risk-${level.toLowerCase()}`;
}

export function statusClass(status: string): string {
  return `status-${status.toLowerCase()}`;
}

/** Compact "totals by currency" rendering, e.g. "12,340.50 EUR · 1.48 BTC". */
export function formatTotals(totals: Record<string, number>): string {
  const entries = Object.entries(totals);
  if (entries.length === 0) return '—';
  return entries.map(([currency, amount]) => formatAmount(amount, currency)).join(' · ');
}

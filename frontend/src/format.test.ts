import { describe, expect, it } from 'vitest';
import { formatAmount, formatDateTime, formatTotals, riskClass } from './format';

describe('format helpers', () => {
  it('formats fiat amounts with two decimals', () => {
    expect(formatAmount(48200, 'EUR')).toBe('48,200.00 EUR');
  });

  it('keeps crypto precision', () => {
    expect(formatAmount(0.31, 'BTC')).toBe('0.31 BTC');
    expect(formatAmount(3.2, 'ETH')).toBe('3.20 ETH');
  });

  it('formats ISO timestamps for display', () => {
    expect(formatDateTime('2026-08-20T02:55:00')).toBe('2026-08-20 02:55');
    expect(formatDateTime(null)).toBe('—');
  });

  it('maps risk levels to css classes', () => {
    expect(riskClass('CRITICAL')).toBe('risk-critical');
    expect(riskClass('LOW')).toBe('risk-low');
  });

  it('joins currency totals', () => {
    expect(formatTotals({ EUR: 100, BTC: 1.5 })).toBe('100.00 EUR · 1.50 BTC');
    expect(formatTotals({})).toBe('—');
  });
});

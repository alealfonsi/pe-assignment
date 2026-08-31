import { Fragment, useEffect, useState } from 'react';
import { getTransactions } from '../api';
import type { TransactionItem, TransactionsPage } from '../types';
import { formatAmount, formatDateTime, statusClass } from '../format';

const TABS = [
  { key: null, label: 'All' },
  { key: 'CARD', label: 'Card' },
  { key: 'PAYMENT', label: 'Payments' },
  { key: 'CRYPTO', label: 'Crypto' },
] as const;

const PAGE_SIZE = 12;

function DetailRow({ tx }: { tx: TransactionItem }) {
  return (
    <tr className="detail-row">
      <td colSpan={5}>
        <div className="detail-grid">
          {tx.card && (
            <>
              <span>Card</span>
              <span>
                {tx.card.cardPan} ({tx.card.cardType})
              </span>
              <span>Merchant</span>
              <span>
                {tx.card.merchantName} · MCC {tx.card.mccCode}
              </span>
              <span>Mode</span>
              <span>{tx.card.cardPresent ? 'Card present' : 'Card not present'}</span>
              {tx.card.authorizationCode && (
                <>
                  <span>Auth code</span>
                  <span className="mono">{tx.card.authorizationCode}</span>
                </>
              )}
              {tx.card.declineReason && (
                <>
                  <span>Decline reason</span>
                  <span className="status-failed">{tx.card.declineReason}</span>
                </>
              )}
            </>
          )}
          {tx.payment && (
            <>
              <span>Method</span>
              <span>{tx.payment.paymentMethod}</span>
              <span>From</span>
              <span className="mono">{tx.payment.senderAccount}</span>
              <span>To</span>
              <span className="mono">
                {tx.payment.receiverAccount} ({tx.payment.receiverBankCountry})
              </span>
            </>
          )}
          {tx.crypto && (
            <>
              <span>Chain</span>
              <span>
                {tx.crypto.blockchain}
                {tx.crypto.exchangeName ? ` · via ${tx.crypto.exchangeName}` : ' · no exchange'}
              </span>
              <span>From</span>
              <span className="mono">{tx.crypto.walletAddressFrom}</span>
              <span>To</span>
              <span className="mono">{tx.crypto.walletAddressTo}</span>
              <span>Tx hash</span>
              <span className="mono small">{tx.crypto.txHash}</span>
            </>
          )}
          {tx.firedRules.length > 0 && (
            <>
              <span>Risk signals</span>
              <span>
                {tx.firedRules.map((r) => (
                  <span className="rule-chip" key={r.ruleName}>
                    {r.ruleName} <span className="rule-score">+{r.scoreContribution}</span>
                  </span>
                ))}
              </span>
            </>
          )}
        </div>
      </td>
    </tr>
  );
}

export default function TransactionsTable({ customerId }: { customerId: string }) {
  const [tab, setTab] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<TransactionsPage | null>(null);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getTransactions(customerId, tab, page, PAGE_SIZE)
      .then((d) => {
        setData(d);
        setError(null);
      })
      .catch((e) => setError(String(e)));
  }, [customerId, tab, page]);

  return (
    <div className="tx-panel">
      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.label}
            className={`tab ${tab === t.key ? 'active' : ''}`}
            onClick={() => {
              setTab(t.key);
              setPage(0);
              setExpanded(null);
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && <div className="alert">{error}</div>}

      <table className="table clickable">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th className="num">Amount</th>
            <th>Status</th>
            <th>Signals</th>
          </tr>
        </thead>
        <tbody>
          {data?.items.map((tx) => (
            <Fragment key={tx.transactionId}>
              <tr
                className={expanded === tx.transactionId ? 'expanded' : ''}
                onClick={() => setExpanded(expanded === tx.transactionId ? null : tx.transactionId)}
              >
                <td className="mono small">{formatDateTime(tx.createdAt)}</td>
                <td>{tx.activityType}</td>
                <td className="num strong">{formatAmount(tx.amount, tx.currency)}</td>
                <td>
                  <span className={statusClass(tx.status)}>{tx.status.toLowerCase()}</span>
                </td>
                <td>{tx.firedRules.length > 0 ? <span className="signal-dot">{tx.firedRules.length}</span> : ''}</td>
              </tr>
              {expanded === tx.transactionId && <DetailRow tx={tx} />}
            </Fragment>
          ))}
          {data && data.items.length === 0 && (
            <tr>
              <td colSpan={5} className="muted center">
                No transactions
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {data && data.totalPages > 1 && (
        <div className="pager">
          <button className="btn btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>
            ← Prev
          </button>
          <span className="muted">
            Page {page + 1} of {data.totalPages} · {data.totalItems} transactions
          </span>
          <button
            className="btn btn-ghost"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}

import type { CustomerOverview } from '../types';
import { formatTotals } from '../format';

const TYPE_LABELS: Record<string, string> = {
  CARD: 'Card activity',
  PAYMENT: 'Payments',
  CRYPTO: 'Crypto activity',
};

export default function SummaryCards({ overview }: { overview: CustomerOverview }) {
  return (
    <>
      <div className="cards">
        {overview.activity.map((a) => (
          <div className="card" key={a.type}>
            <div className="card-title">{TYPE_LABELS[a.type] ?? a.type}</div>
            <div className="card-value">{a.count}</div>
            <div className="card-sub">{formatTotals(a.totalsByCurrency)} completed</div>
            <div className="card-statuses">
              <span className="status-completed">{a.completed} ok</span>
              {a.pending > 0 && <span className="status-pending">{a.pending} pending</span>}
              {a.failed > 0 && <span className="status-failed">{a.failed} failed</span>}
              {a.reversed > 0 && <span className="status-reversed">{a.reversed} reversed</span>}
            </div>
          </div>
        ))}
        {overview.activity.length === 0 && <div className="card muted">No activity recorded</div>}
      </div>

      {overview.firedRules.length > 0 && (
        <div className="fired-rules">
          <div className="fired-rules-title">Automated risk signals</div>
          <div className="fired-rules-list">
            {overview.firedRules.map((r) => (
              <span className="rule-chip" key={r.ruleName} title={`applies to ${r.appliesTo}`}>
                {r.ruleName} <b>×{r.timesFired}</b> <span className="rule-score">+{r.totalContribution}</span>
              </span>
            ))}
          </div>
        </div>
      )}
    </>
  );
}

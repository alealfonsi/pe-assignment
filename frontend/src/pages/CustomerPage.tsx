import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getAnalysisHistory, getCustomerOverview } from '../api';
import type { AnalysisResponse, CustomerOverview } from '../types';
import SummaryCards from '../components/SummaryCards';
import TransactionsTable from '../components/TransactionsTable';
import AnalysisPanel from '../components/AnalysisPanel';
import { formatDateTime } from '../format';

export default function CustomerPage() {
  const { customerId } = useParams<{ customerId: string }>();
  const [overview, setOverview] = useState<CustomerOverview | null>(null);
  const [history, setHistory] = useState<AnalysisResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  const reloadHistory = useCallback(() => {
    if (!customerId) return;
    getAnalysisHistory(customerId)
      .then(setHistory)
      .catch((e) => setError(String(e)));
  }, [customerId]);

  useEffect(() => {
    if (!customerId) return;
    getCustomerOverview(customerId)
      .then((o) => {
        setOverview(o);
        setError(null);
      })
      .catch((e) => setError(String(e)));
    reloadHistory();
  }, [customerId, reloadHistory]);

  if (error) {
    return (
      <div className="page">
        <div className="alert">{error}</div>
        <Link to="/customers">← Back to search</Link>
      </div>
    );
  }
  if (!overview || !customerId) {
    return <div className="page muted">Loading…</div>;
  }

  return (
    <div className="page">
      <div className="breadcrumbs">
        <Link to="/customers">Customers</Link> <span>/</span> <span>{overview.fullName}</span>
      </div>

      <header className="customer-header">
        <div>
          <h1>{overview.fullName}</h1>
          <div className="muted mono">{overview.customerId}</div>
          <div className="customer-meta">
            <span className="pill">{overview.segment}</span>
            <span className="pill">{overview.country}</span>
            <span className="muted">{overview.email}</span>
            <span className="muted">customer since {formatDateTime(overview.customerSince).slice(0, 10)}</span>
          </div>
        </div>
        <div className="risk-score-box">
          <div className="risk-score-label">Rule-based risk score</div>
          <div className={`risk-score ${overview.riskScore >= 90 ? 'crit' : overview.riskScore >= 50 ? 'high' : overview.riskScore >= 20 ? 'med' : 'low'}`}>
            {overview.riskScore}
          </div>
          <div className="muted small">
            {overview.totalTransactions} tx · {formatDateTime(overview.firstActivityAt).slice(0, 10)} →{' '}
            {formatDateTime(overview.lastActivityAt).slice(0, 10)}
          </div>
        </div>
      </header>

      <SummaryCards overview={overview} />

      <div className="columns">
        <section className="col-main">
          <h2>Activity</h2>
          <TransactionsTable customerId={customerId} />
        </section>
        <section className="col-side">
          <AnalysisPanel customerId={customerId} history={history} onNewAnalysis={reloadHistory} />
        </section>
      </div>
    </div>
  );
}

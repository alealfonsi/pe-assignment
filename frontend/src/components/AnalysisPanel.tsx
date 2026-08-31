import { useEffect, useState } from 'react';
import { runAnalysis } from '../api';
import type { AnalysisResponse } from '../types';
import { formatDateTime, riskClass } from '../format';

function AnalysisView({ analysis }: { analysis: AnalysisResponse }) {
  const [showSources, setShowSources] = useState(false);
  const [showSignals, setShowSignals] = useState(false);

  return (
    <div className="analysis">
      <div className="analysis-head">
        <span className={`risk-badge ${riskClass(analysis.riskLevel)}`}>{analysis.riskLevel}</span>
        <div className="analysis-meta">
          <div>
            {formatDateTime(analysis.createdAt)} · {analysis.operatorName}
          </div>
          <div className="muted small">
            {analysis.llmMode === 'ANTHROPIC' ? `Claude (${analysis.model})` : `stub (${analysis.model})`} ·{' '}
            {(analysis.latencyMs / 1000).toFixed(1)}s
            {analysis.inputTokens != null && ` · ${analysis.inputTokens}→${analysis.outputTokens} tokens`}
          </div>
        </div>
      </div>

      <p className="analysis-summary">{analysis.summary}</p>

      {analysis.findings.length > 0 && (
        <>
          <h3>Findings</h3>
          <ul className="findings">
            {analysis.findings.map((f, i) => (
              <li key={i}>
                <div className="finding-head">
                  <span className={`severity-dot ${riskClass(f.severity)}`} title={f.severity} />
                  <span className="strong">{f.title}</span>
                </div>
                <div className="finding-evidence">{f.evidence}</div>
                {f.policyRefs.length > 0 && (
                  <div className="policy-refs">
                    {f.policyRefs.map((ref) => (
                      <span className="policy-ref" key={ref}>
                        § {ref}
                      </span>
                    ))}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </>
      )}

      <h3>Recommendations</h3>
      <ol className="recommendations">
        {analysis.recommendations.map((r, i) => (
          <li key={i}>{r}</li>
        ))}
      </ol>

      <button className="link-toggle" onClick={() => setShowSignals(!showSignals)}>
        {showSignals ? '▾' : '▸'} Triage signals & retrieval queries
      </button>
      {showSignals && (
        <div className="signals-box">
          <div className="small strong">Salient signals (LLM call 1)</div>
          <ul className="small">
            {analysis.salientSignals.map((s, i) => (
              <li key={i}>{s}</li>
            ))}
          </ul>
          <div className="small strong">Policy KB queries</div>
          <ul className="small mono">
            {analysis.retrievalQueries.map((q, i) => (
              <li key={i}>{q}</li>
            ))}
          </ul>
        </div>
      )}

      <button className="link-toggle" onClick={() => setShowSources(!showSources)}>
        {showSources ? '▾' : '▸'} Policy sources used ({analysis.sources.length})
      </button>
      {showSources && (
        <div className="sources">
          {analysis.sources.map((s, i) => (
            <div className="source" key={i}>
              <div className="source-title">
                [S{i + 1}] {s.documentTitle}
                {s.sectionTitle ? ` — ${s.sectionTitle}` : ''}
              </div>
              <div className="source-excerpt">{s.excerpt}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function AnalysisPanel({
  customerId,
  history,
  onNewAnalysis,
}: {
  customerId: string;
  history: AnalysisResponse[];
  onNewAnalysis: () => void;
}) {
  const [current, setCurrent] = useState<AnalysisResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Show the latest persisted analysis when the page loads or history refreshes.
  useEffect(() => {
    if (!current && history.length > 0) {
      setCurrent(history[0]);
    }
  }, [history, current]);

  async function run() {
    setBusy(true);
    setError(null);
    try {
      const analysis = await runAnalysis(customerId);
      setCurrent(analysis);
      onNewAnalysis();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="analysis-panel">
      <div className="analysis-panel-head">
        <h2>AI risk analysis</h2>
        <button className="btn btn-primary" onClick={run} disabled={busy}>
          {busy ? 'Analyzing…' : current ? 'Run new analysis' : 'Run AI analysis'}
        </button>
      </div>

      {busy && (
        <div className="progress muted small">
          Building activity digest → triage (LLM) → policy retrieval → risk analysis (LLM)…
        </div>
      )}
      {error && <div className="alert">{error}</div>}

      {history.length > 1 && (
        <select
          className="history-select"
          value={current?.analysisId ?? ''}
          onChange={(e) => {
            const found = history.find((h) => h.analysisId === e.target.value);
            if (found) setCurrent(found);
          }}
        >
          {history.map((h) => (
            <option key={h.analysisId} value={h.analysisId}>
              {formatDateTime(h.createdAt)} — {h.riskLevel} — {h.operatorName}
            </option>
          ))}
        </select>
      )}

      {current ? (
        <AnalysisView analysis={current} />
      ) : (
        !busy && (
          <p className="muted">
            No analysis yet for this customer. Run one to get a risk level, findings grounded in the
            activity data, and policy-based recommendations.
          </p>
        )
      )}
    </div>
  );
}

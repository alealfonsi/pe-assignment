import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchCustomers } from '../api';
import type { CustomerSearchItem } from '../types';

export default function SearchPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<CustomerSearchItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handle = setTimeout(() => {
      searchCustomers(query)
        .then((items) => {
          setResults(items);
          setError(null);
        })
        .catch((e) => setError(String(e)));
    }, 200);
    return () => clearTimeout(handle);
  }, [query]);

  return (
    <div className="page">
      <header className="page-header">
        <h1>Customers</h1>
        <p className="muted">Search by customer ID or name, then open the activity dashboard.</p>
      </header>

      <input
        className="search-input"
        placeholder="Search by customer ID or name…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        autoFocus
      />

      {error && <div className="alert">{error}</div>}

      {results && (
        <table className="table clickable">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Customer ID</th>
              <th>Segment</th>
              <th>Country</th>
              <th className="num">Transactions</th>
            </tr>
          </thead>
          <tbody>
            {results.map((c) => (
              <tr key={c.customerId} onClick={() => navigate(`/customers/${c.customerId}`)}>
                <td className="strong">{c.fullName}</td>
                <td className="mono">{c.customerId}</td>
                <td>{c.segment}</td>
                <td>{c.country}</td>
                <td className="num">{c.transactionCount}</td>
              </tr>
            ))}
            {results.length === 0 && (
              <tr>
                <td colSpan={5} className="muted center">
                  No customers match “{query}”
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}

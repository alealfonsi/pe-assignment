import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, login } from '../api';

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(username, password);
      navigate('/customers');
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? 'Invalid username or password' : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={onSubmit}>
        <div className="brand login-brand">
          <span className="brand-mark">◆</span>
          <div>
            <div className="brand-name">CAA Console</div>
            <div className="brand-sub">Customer Activity Analytics</div>
          </div>
        </div>
        <h1>Operator sign in</h1>
        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && <div className="alert">{error}</div>}
        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="login-hint">Demo operators: alice / operator1 · bob / operator2 · carol / supervisor1</p>
      </form>
    </div>
  );
}

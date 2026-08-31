import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { clearSession, getOperator, getToken } from './api';
import LoginPage from './pages/LoginPage';
import SearchPage from './pages/SearchPage';
import CustomerPage from './pages/CustomerPage';

function RequireAuth({ children }: { children: React.ReactNode }) {
  if (!getToken()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function Shell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const operator = getOperator();
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand" onClick={() => navigate('/customers')}>
          <span className="brand-mark">◆</span>
          <div>
            <div className="brand-name">CAA Console</div>
            <div className="brand-sub">Customer Activity Analytics</div>
          </div>
        </div>
        <nav>
          <button className="nav-item active" onClick={() => navigate('/customers')}>
            Customers
          </button>
        </nav>
        <div className="sidebar-footer">
          {operator && (
            <div className="operator-chip">
              <div className="operator-name">{operator.displayName}</div>
              <div className="operator-role">{operator.role}</div>
            </div>
          )}
          <button
            className="btn btn-ghost"
            onClick={() => {
              clearSession();
              navigate('/login');
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/customers"
        element={
          <RequireAuth>
            <Shell>
              <SearchPage />
            </Shell>
          </RequireAuth>
        }
      />
      <Route
        path="/customers/:customerId"
        element={
          <RequireAuth>
            <Shell>
              <CustomerPage />
            </Shell>
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/customers" replace />} />
    </Routes>
  );
}

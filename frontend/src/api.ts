import type {
  AnalysisResponse,
  CustomerOverview,
  CustomerSearchItem,
  LoginResponse,
  TransactionsPage,
} from './types';

const TOKEN_KEY = 'caa.token';
const OPERATOR_KEY = 'caa.operator';

export interface StoredOperator {
  username: string;
  displayName: string;
  role: string;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getOperator(): StoredOperator | null {
  const raw = localStorage.getItem(OPERATOR_KEY);
  return raw ? (JSON.parse(raw) as StoredOperator) : null;
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(OPERATOR_KEY);
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init?.headers as Record<string, string>),
  };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(path, { ...init, headers });
  if (response.status === 401 && !path.endsWith('/auth/login')) {
    clearSession();
    window.location.href = '/login';
    throw new ApiError(401, 'Session expired');
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) message = body.message;
    } catch {
      // keep default message
    }
    throw new ApiError(response.status, message);
  }
  return (await response.json()) as T;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const result = await request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  localStorage.setItem(TOKEN_KEY, result.token);
  localStorage.setItem(
    OPERATOR_KEY,
    JSON.stringify({ username: result.username, displayName: result.displayName, role: result.role }),
  );
  return result;
}

export function searchCustomers(query: string): Promise<CustomerSearchItem[]> {
  const params = query ? `?query=${encodeURIComponent(query)}` : '';
  return request<CustomerSearchItem[]>(`/api/customers${params}`);
}

export function getCustomerOverview(customerId: string): Promise<CustomerOverview> {
  return request<CustomerOverview>(`/api/customers/${customerId}`);
}

export function getTransactions(
  customerId: string,
  type: string | null,
  page: number,
  size: number,
): Promise<TransactionsPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (type) params.set('type', type);
  return request<TransactionsPage>(`/api/customers/${customerId}/transactions?${params}`);
}

export function runAnalysis(customerId: string): Promise<AnalysisResponse> {
  return request<AnalysisResponse>(`/api/customers/${customerId}/analyses`, { method: 'POST' });
}

export function getAnalysisHistory(customerId: string): Promise<AnalysisResponse[]> {
  return request<AnalysisResponse[]>(`/api/customers/${customerId}/analyses`);
}

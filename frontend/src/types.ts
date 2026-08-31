export type ActivityType = 'CARD' | 'PAYMENT' | 'CRYPTO';
export type TransactionStatus = 'COMPLETED' | 'PENDING' | 'FAILED' | 'REVERSED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface LoginResponse {
  token: string;
  username: string;
  displayName: string;
  role: string;
}

export interface CustomerSearchItem {
  customerId: string;
  fullName: string;
  segment: string;
  country: string;
  transactionCount: number;
}

export interface ActivityTypeSummary {
  type: ActivityType;
  count: number;
  totalsByCurrency: Record<string, number>;
  completed: number;
  pending: number;
  failed: number;
  reversed: number;
}

export interface FiredRuleSummary {
  ruleName: string;
  appliesTo: string;
  timesFired: number;
  totalContribution: number;
}

export interface CustomerOverview {
  customerId: string;
  fullName: string;
  email: string;
  segment: string;
  country: string;
  customerSince: string;
  riskScore: number;
  totalTransactions: number;
  firstActivityAt: string | null;
  lastActivityAt: string | null;
  activity: ActivityTypeSummary[];
  firedRules: FiredRuleSummary[];
}

export interface CardDetails {
  cardPan: string;
  cardType: string;
  merchantName: string;
  mccCode: string;
  cardPresent: boolean;
  authorizationCode: string | null;
  declineReason: string | null;
}

export interface PaymentDetails {
  paymentMethod: string;
  senderAccount: string;
  receiverAccount: string;
  receiverBankCountry: string;
}

export interface CryptoDetails {
  blockchain: string;
  walletAddressFrom: string;
  walletAddressTo: string;
  txHash: string;
  exchangeName: string | null;
}

export interface FiredRule {
  ruleName: string;
  scoreContribution: number;
}

export interface TransactionItem {
  transactionId: string;
  activityType: ActivityType;
  amount: number;
  currency: string;
  status: TransactionStatus;
  createdAt: string;
  card: CardDetails | null;
  payment: PaymentDetails | null;
  crypto: CryptoDetails | null;
  firedRules: FiredRule[];
}

export interface TransactionsPage {
  items: TransactionItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Finding {
  title: string;
  severity: RiskLevel;
  evidence: string;
  policyRefs: string[];
}

export interface SourceRef {
  documentTitle: string;
  sectionTitle: string | null;
  excerpt: string;
}

export interface AnalysisResponse {
  analysisId: string;
  customerId: string;
  operatorName: string;
  createdAt: string;
  llmMode: 'ANTHROPIC' | 'STUB';
  model: string;
  riskLevel: RiskLevel;
  summary: string;
  findings: Finding[];
  recommendations: string[];
  salientSignals: string[];
  retrievalQueries: string[];
  sources: SourceRef[];
  inputTokens: number | null;
  outputTokens: number | null;
  latencyMs: number;
}

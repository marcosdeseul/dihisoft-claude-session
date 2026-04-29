import * as tokenStore from '../auth/tokenStore';
import type { ApiErrorResponse } from './auth';

export interface IssuedApiKeyResponse {
  id: number;
  label: string;
  prefix: string;
  key: string;
  createdAt: string;
}

export interface ApiKeySummaryResponse {
  id: number;
  label: string;
  prefix: string;
  lastUsedAt: string | null;
  createdAt: string;
  revokedAt: string | null;
}

export class ApiKeyError extends Error {
  readonly status: number;
  readonly response: ApiErrorResponse | null;
  constructor(status: number, message: string, response: ApiErrorResponse | null) {
    super(message);
    this.name = 'ApiKeyError';
    this.status = status;
    this.response = response;
  }
}

function authHeaders(): Record<string, string> {
  const token = tokenStore.get();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function toError(res: Response): Promise<ApiKeyError> {
  let body: ApiErrorResponse | null = null;
  try {
    body = (await res.json()) as ApiErrorResponse;
  } catch {
    body = null;
  }
  const message = body?.message || `요청을 처리하지 못했습니다 (HTTP ${res.status})`;
  return new ApiKeyError(res.status, message, body);
}

export async function issueApiKey(label: string): Promise<IssuedApiKeyResponse> {
  const res = await fetch('/api/me/api-keys', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ label }),
  });
  if (res.status === 201) return (await res.json()) as IssuedApiKeyResponse;
  throw await toError(res);
}

export async function listApiKeys(): Promise<ApiKeySummaryResponse[]> {
  const res = await fetch('/api/me/api-keys', { method: 'GET', headers: authHeaders() });
  if (res.ok) return (await res.json()) as ApiKeySummaryResponse[];
  throw await toError(res);
}

export async function revokeApiKey(id: number): Promise<void> {
  const res = await fetch(`/api/me/api-keys/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (res.status === 204) return;
  throw await toError(res);
}

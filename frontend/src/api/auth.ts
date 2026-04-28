export interface SignupRequest {
  username: string;
  password: string;
}

export interface SignupResponse {
  id: number;
  username: string;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
}

export class SignupError extends Error {
  readonly status: number;
  readonly response: ApiErrorResponse | null;
  constructor(status: number, message: string, response: ApiErrorResponse | null) {
    super(message);
    this.name = 'SignupError';
    this.status = status;
    this.response = response;
  }
}

export async function signup(request: SignupRequest): Promise<SignupResponse> {
  const res = await fetch('/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(request),
  });
  if (res.status === 201) {
    return (await res.json()) as SignupResponse;
  }
  let body: ApiErrorResponse | null = null;
  try {
    body = (await res.json()) as ApiErrorResponse;
  } catch {
    body = null;
  }
  const message = body?.message || `요청을 처리하지 못했습니다 (HTTP ${res.status})`;
  throw new SignupError(res.status, message, body);
}

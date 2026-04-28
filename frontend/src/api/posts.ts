import * as tokenStore from '../auth/tokenStore';
import type { ApiErrorResponse } from './auth';

export interface PostResponse {
  id: number;
  title: string;
  content: string;
  authorUsername: string;
  createdAt: string;
  updatedAt: string;
}

export interface PostPageResponse {
  content: PostResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PostWriteRequest {
  title: string;
  content: string;
}

export class PostError extends Error {
  readonly status: number;
  readonly response: ApiErrorResponse | null;
  constructor(status: number, message: string, response: ApiErrorResponse | null) {
    super(message);
    this.name = 'PostError';
    this.status = status;
    this.response = response;
  }
}

function authHeaders(): Record<string, string> {
  const token = tokenStore.get();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function readError(res: Response): Promise<PostError> {
  let body: ApiErrorResponse | null = null;
  try {
    body = (await res.json()) as ApiErrorResponse;
  } catch {
    body = null;
  }
  const message = body?.message || `요청을 처리하지 못했습니다 (HTTP ${res.status})`;
  return new PostError(res.status, message, body);
}

export async function listPosts(page: number = 0, size: number = 10): Promise<PostPageResponse> {
  const res = await fetch(`/api/posts?page=${page}&size=${size}`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
  });
  if (res.ok) return (await res.json()) as PostPageResponse;
  throw await readError(res);
}

export async function getPost(id: number): Promise<PostResponse> {
  const res = await fetch(`/api/posts/${id}`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
  });
  if (res.ok) return (await res.json()) as PostResponse;
  throw await readError(res);
}

export async function createPost(request: PostWriteRequest): Promise<PostResponse> {
  const res = await fetch('/api/posts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...authHeaders() },
    body: JSON.stringify(request),
  });
  if (res.status === 201) return (await res.json()) as PostResponse;
  throw await readError(res);
}

export async function updatePost(id: number, request: PostWriteRequest): Promise<PostResponse> {
  const res = await fetch(`/api/posts/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...authHeaders() },
    body: JSON.stringify(request),
  });
  if (res.ok) return (await res.json()) as PostResponse;
  throw await readError(res);
}

export async function deletePost(id: number): Promise<void> {
  const res = await fetch(`/api/posts/${id}`, {
    method: 'DELETE',
    headers: { Accept: 'application/json', ...authHeaders() },
  });
  if (res.status === 204) return;
  throw await readError(res);
}

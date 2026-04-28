import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import PostEditPage from './PostEditPage';
import * as tokenStore from '../auth/tokenStore';

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  tokenStore.clear();
});

beforeEach(() => {
  delete (globalThis as { fetch?: typeof fetch }).fetch;
  tokenStore.clear();
});

interface MockResponse {
  status: number;
  body: unknown;
}

function mockFetchSequence(responses: MockResponse[]) {
  let i = 0;
  const fn = vi.fn(() => {
    const r = responses[Math.min(i, responses.length - 1)];
    i += 1;
    return Promise.resolve({
      ok: r.status >= 200 && r.status < 300,
      status: r.status,
      json: async () => r.body,
    } as Response);
  });
  globalThis.fetch = fn as unknown as typeof fetch;
  return fn;
}

let currentPath: string | null = null;
function LocationSpy() {
  const loc = useLocation();
  currentPath = loc.pathname;
  return null;
}

function renderEditPage(id: number = 7) {
  currentPath = null;
  return render(
    <MemoryRouter initialEntries={[`/posts/${id}/edit`]}>
      <Routes>
        <Route path="/posts/:id/edit" element={<PostEditPage />} />
        <Route path="/posts/:id" element={<div data-testid="detail" />} />
        <Route path="/login" element={<div data-testid="login">LOGIN</div>} />
      </Routes>
      <LocationSpy />
    </MemoryRouter>,
  );
}

const post = (overrides: Partial<{ title: string; content: string }> = {}) => ({
  id: 7,
  title: '원래 제목',
  content: '원래 본문',
  authorUsername: 'marco',
  createdAt: '2026-04-28T00:00:00Z',
  updatedAt: '2026-04-28T00:00:00Z',
  ...overrides,
});

const errorBody = (status: number, message: string) => ({
  status,
  error: 'Error',
  message,
  path: '/api/posts/7',
  timestamp: '2026-04-28T00:00:00Z',
});

async function flush() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

async function clickSubmit(name: RegExp = /^수정$/) {
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name }));
  });
}

describe('PostEditPage', () => {
  it('비로그인_상태에서_접근_시_/login_으로_redirect_한다', () => {
    renderEditPage(7);
    expect(currentPath).toBe('/login');
  });

  it('마운트_시_GET_/api/posts/:id_로_입력값을_prefill_한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([{ status: 200, body: post() }]);
    await act(async () => {
      renderEditPage(7);
    });
    await flush();
    const titleInput = screen.getByLabelText(/^title$/i) as HTMLInputElement;
    const contentInput = screen.getByLabelText(/^content$/i) as HTMLTextAreaElement;
    expect(titleInput.value).toBe('원래 제목');
    expect(contentInput.value).toBe('원래 본문');
  });

  it('prefill_404이면_에러_배너를_렌더한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([{ status: 404, body: errorBody(404, '게시글을 찾을 수 없습니다: 7') }]);
    await act(async () => {
      renderEditPage(7);
    });
    await flush();
    expect(screen.getByText(/게시글을 찾을 수 없습니다/)).toBeTruthy();
  });

  it('정상_제출이면_PUT_/api/posts/:id_을_Bearer_헤더와_함께_호출한다', async () => {
    tokenStore.set('t.k.n');
    const fn = mockFetchSequence([
      { status: 200, body: post() },
      { status: 200, body: post({ title: '바뀐 제목', content: '바뀐 본문' }) },
    ]);
    await act(async () => {
      renderEditPage(7);
    });
    await flush();
    fireEvent.change(screen.getByLabelText(/^title$/i), { target: { value: '바뀐 제목' } });
    fireEvent.change(screen.getByLabelText(/^content$/i), { target: { value: '바뀐 본문' } });
    await clickSubmit();
    await flush();
    const [url, init] = fn.mock.calls[1] as [string, RequestInit];
    expect(url).toBe('/api/posts/7');
    expect(init.method).toBe('PUT');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer t.k.n');
    expect(JSON.parse(init.body as string)).toEqual({ title: '바뀐 제목', content: '바뀐 본문' });
    expect(currentPath).toBe('/posts/7');
  });

  it('title을_빈_값으로_제출하면_검증_메시지가_뜨고_PUT은_호출되지_않는다', async () => {
    tokenStore.set('t.k.n');
    const fn = mockFetchSequence([{ status: 200, body: post() }]);
    await act(async () => {
      renderEditPage(7);
    });
    await flush();
    fireEvent.change(screen.getByLabelText(/^title$/i), { target: { value: '' } });
    await clickSubmit();
    expect(fn).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/title은 필수입니다/)).toBeTruthy();
  });

  it('403_응답_시_본인의_글만_메시지를_렌더한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([
      { status: 200, body: post() },
      { status: 403, body: errorBody(403, '본인의 글만 수정/삭제할 수 있습니다') },
    ]);
    await act(async () => {
      renderEditPage(7);
    });
    await flush();
    await clickSubmit();
    await flush();
    expect(screen.getByText(/본인의 글만/)).toBeTruthy();
    expect(currentPath).toBe('/posts/7/edit');
  });

  it('401_응답_시_/login_으로_redirect_한다', async () => {
    tokenStore.set('expired');
    mockFetchSequence([
      { status: 200, body: post() },
      { status: 401, body: errorBody(401, '인증이 필요합니다') },
    ]);
    await act(async () => {
      renderEditPage(7);
    });
    await flush();
    await clickSubmit();
    await flush();
    expect(currentPath).toBe('/login');
  });
});

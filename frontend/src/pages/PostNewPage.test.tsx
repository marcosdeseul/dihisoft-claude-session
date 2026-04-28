import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import PostNewPage from './PostNewPage';
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

function renderNewPage() {
  currentPath = null;
  return render(
    <MemoryRouter initialEntries={['/posts/new']}>
      <Routes>
        <Route path="/posts/new" element={<PostNewPage />} />
        <Route path="/posts/:id" element={<div data-testid="detail" />} />
        <Route path="/login" element={<div data-testid="login">LOGIN</div>} />
      </Routes>
      <LocationSpy />
    </MemoryRouter>,
  );
}

function fill(title: string, content: string) {
  fireEvent.change(screen.getByLabelText(/^title$/i), { target: { value: title } });
  fireEvent.change(screen.getByLabelText(/^content$/i), { target: { value: content } });
}

async function clickSubmit(name: RegExp = /^등록$/) {
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name }));
  });
}

const errorBody = (status: number, message: string) => ({
  status,
  error: 'Error',
  message,
  path: '/api/posts',
  timestamp: '2026-04-28T00:00:00Z',
});

const created = (id: number, title: string, content: string) => ({
  id,
  title,
  content,
  authorUsername: 'marco',
  createdAt: '2026-04-28T00:00:00Z',
  updatedAt: '2026-04-28T00:00:00Z',
});

async function flush() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

describe('PostNewPage', () => {
  it('비로그인_상태에서_/posts/new_접근_시_/login_으로_redirect_한다', () => {
    renderNewPage();
    expect(currentPath).toBe('/login');
  });

  it('정상_제출이면_POST_/api/posts_를_Bearer_헤더와_함께_호출한다', async () => {
    tokenStore.set('t.k.n');
    const fn = mockFetchSequence([{ status: 201, body: created(11, '새 글', '본문') }]);
    renderNewPage();
    fill('새 글', '본문');
    await clickSubmit();
    await flush();
    expect(fn).toHaveBeenCalledTimes(1);
    const [url, init] = fn.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/posts');
    expect(init.method).toBe('POST');
    const headers = init.headers as Record<string, string>;
    expect(headers['Content-Type']).toBe('application/json');
    expect(headers.Authorization).toBe('Bearer t.k.n');
    expect(JSON.parse(init.body as string)).toEqual({ title: '새 글', content: '본문' });
  });

  it('201_응답_시_/posts/:id_로_네비게이트한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([{ status: 201, body: created(11, '새 글', '본문') }]);
    renderNewPage();
    fill('새 글', '본문');
    await clickSubmit();
    await flush();
    expect(currentPath).toBe('/posts/11');
  });

  it('title이_빈_값이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    tokenStore.set('t.k.n');
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderNewPage();
    fill('', '본문');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText(/title은 필수입니다/)).toBeTruthy();
  });

  it('title이_공백만이면_검증_메시지가_뜬다', async () => {
    tokenStore.set('t.k.n');
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderNewPage();
    fill('   ', '본문');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText(/title은 필수입니다/)).toBeTruthy();
  });

  it('title이_101자이면_검증_메시지가_뜬다', async () => {
    tokenStore.set('t.k.n');
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderNewPage();
    fill('a'.repeat(101), '본문');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText(/title은 최대 100자입니다/)).toBeTruthy();
  });

  it('content가_빈_값이면_검증_메시지가_뜬다', async () => {
    tokenStore.set('t.k.n');
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderNewPage();
    fill('제목', '');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText(/content는 필수입니다/)).toBeTruthy();
  });

  it('400_응답_시_서버_message를_렌더한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([{ status: 400, body: errorBody(400, 'title: title은 최대 100자입니다') }]);
    renderNewPage();
    fill('제목', '본문');
    await clickSubmit();
    await flush();
    expect(screen.getByText(/title은 최대 100자입니다/)).toBeTruthy();
    expect(currentPath).toBe('/posts/new');
  });

  it('401_응답_시_/login_으로_redirect_한다_만료_토큰', async () => {
    tokenStore.set('expired');
    mockFetchSequence([{ status: 401, body: errorBody(401, '인증이 필요합니다') }]);
    renderNewPage();
    fill('제목', '본문');
    await clickSubmit();
    await flush();
    expect(currentPath).toBe('/login');
  });

  it('제출_중에는_버튼이_disabled_이다', async () => {
    tokenStore.set('t.k.n');
    let resolveFetch: (value: Response) => void = () => {};
    const pending = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });
    globalThis.fetch = vi.fn(() => pending) as unknown as typeof fetch;
    renderNewPage();
    fill('제목', '본문');
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^등록$/ }));
    });
    const button = screen.getByRole('button', { name: /등록 중/ }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    await act(async () => {
      resolveFetch({
        ok: true,
        status: 201,
        json: async () => created(11, '제목', '본문'),
      } as Response);
      await pending;
    });
  });
});

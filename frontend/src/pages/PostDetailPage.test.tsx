import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import PostDetailPage from './PostDetailPage';
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

function renderDetailPage(id: number = 7) {
  currentPath = null;
  return render(
    <MemoryRouter initialEntries={[`/posts/${id}`]}>
      <Routes>
        <Route path="/posts/:id" element={<PostDetailPage />} />
        <Route path="/posts/:id/edit" element={<div data-testid="edit" />} />
        <Route path="/" element={<div data-testid="home">HOME</div>} />
        <Route path="/login" element={<div data-testid="login">LOGIN</div>} />
      </Routes>
      <LocationSpy />
    </MemoryRouter>,
  );
}

const post = (overrides: Partial<{ id: number; title: string; content: string; authorUsername: string }> = {}) => ({
  id: 7,
  title: '오늘의 글',
  content: '내용\n두 번째 줄',
  authorUsername: 'marco',
  createdAt: '2026-04-28T00:00:00Z',
  updatedAt: '2026-04-28T00:00:00Z',
  ...overrides,
});

const errorBody = (status: number, message: string, path = '/api/posts/7') => ({
  status,
  error: 'Error',
  message,
  path,
  timestamp: '2026-04-28T00:00:00Z',
});

async function flush() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

describe('PostDetailPage', () => {
  it('마운트_시_GET_/api/posts/:id_로_본문을_가져온다', async () => {
    const fn = mockFetchSequence([{ status: 200, body: post() }]);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    const [url] = fn.mock.calls[0] as [string, RequestInit | undefined];
    expect(url).toBe('/api/posts/7');
    expect(screen.getByText('오늘의 글')).toBeTruthy();
    expect(screen.getByText(/marco/)).toBeTruthy();
  });

  it('404_응답이면_없음_안내를_렌더한다', async () => {
    mockFetchSequence([{ status: 404, body: errorBody(404, '게시글을 찾을 수 없습니다: 7') }]);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    expect(screen.getByText(/게시글을 찾을 수 없습니다/)).toBeTruthy();
  });

  it('비로그인이면_수정_삭제_버튼이_안_보인다', async () => {
    mockFetchSequence([{ status: 200, body: post() }]);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    expect(screen.queryByRole('link', { name: /수정/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /삭제/ })).toBeNull();
  });

  it('로그인_상태면_수정_링크와_삭제_버튼이_보인다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([{ status: 200, body: post() }]);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    expect(screen.getByRole('link', { name: /수정/ })).toBeTruthy();
    expect(screen.getByRole('button', { name: /^삭제$/ })).toBeTruthy();
  });

  it('수정_링크는_/posts/:id/edit_로_라우팅한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([{ status: 200, body: post() }]);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('link', { name: /수정/ }));
    });
    expect(currentPath).toBe('/posts/7/edit');
  });

  it('삭제_버튼_클릭_시_confirm_확인_후_DELETE_요청하고_홈으로_이동한다', async () => {
    tokenStore.set('t.k.n');
    const fn = mockFetchSequence([
      { status: 200, body: post() },
      { status: 204, body: null },
    ]);
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^삭제$/ }));
    });
    await flush();
    expect(confirmSpy).toHaveBeenCalled();
    expect(fn).toHaveBeenCalledTimes(2);
    const [url, init] = fn.mock.calls[1] as [string, RequestInit];
    expect(url).toBe('/api/posts/7');
    expect(init.method).toBe('DELETE');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer t.k.n');
    expect(currentPath).toBe('/');
  });

  it('삭제_confirm_취소_시_요청하지_않는다', async () => {
    tokenStore.set('t.k.n');
    const fn = mockFetchSequence([{ status: 200, body: post() }]);
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^삭제$/ }));
    });
    await flush();
    expect(fn).toHaveBeenCalledTimes(1);
    expect(currentPath).toBe('/posts/7');
  });

  it('삭제_시_403_이면_본인의_글만_메시지를_렌더한다', async () => {
    tokenStore.set('t.k.n');
    mockFetchSequence([
      { status: 200, body: post() },
      { status: 403, body: errorBody(403, '본인의 글만 수정/삭제할 수 있습니다') },
    ]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^삭제$/ }));
    });
    await flush();
    expect(screen.getByText(/본인의 글만/)).toBeTruthy();
    expect(currentPath).toBe('/posts/7');
  });

  it('삭제_시_401_이면_/login_으로_redirect_한다', async () => {
    tokenStore.set('expired');
    mockFetchSequence([
      { status: 200, body: post() },
      { status: 401, body: errorBody(401, '인증이 필요합니다') },
    ]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await act(async () => {
      renderDetailPage(7);
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^삭제$/ }));
    });
    await flush();
    expect(currentPath).toBe('/login');
  });
});

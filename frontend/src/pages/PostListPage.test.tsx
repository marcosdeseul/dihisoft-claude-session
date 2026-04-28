import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import PostListPage from './PostListPage';
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

function renderListPage() {
  currentPath = null;
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<PostListPage />} />
        <Route path="/posts/:id" element={<div data-testid="detail" />} />
        <Route path="/posts/new" element={<div data-testid="new" />} />
      </Routes>
      <LocationSpy />
    </MemoryRouter>,
  );
}

const listBody = (overrides: Partial<{ content: unknown[]; page: number; size: number; totalElements: number; totalPages: number }> = {}) => ({
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  ...overrides,
});

const samplePost = (id: number, overrides: Partial<{ title: string; authorUsername: string; createdAt: string }> = {}) => ({
  id,
  title: `글 ${id}`,
  content: `본문 ${id}`,
  authorUsername: 'marco',
  createdAt: '2026-04-28T00:00:00Z',
  updatedAt: '2026-04-28T00:00:00Z',
  ...overrides,
});

async function flush() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

describe('PostListPage', () => {
  it('마운트_시_GET_/api/posts_를_page0_size10_으로_호출한다', async () => {
    const fn = mockFetchSequence([
      { status: 200, body: listBody({ content: [samplePost(1)], totalElements: 1, totalPages: 1 }) },
    ]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    expect(fn).toHaveBeenCalledTimes(1);
    const [url, init] = fn.mock.calls[0] as [string, RequestInit | undefined];
    expect(url).toBe('/api/posts?page=0&size=10');
    expect(init?.method ?? 'GET').toBe('GET');
  });

  it('빈_목록이면_안내_문구를_렌더한다', async () => {
    mockFetchSequence([{ status: 200, body: listBody() }]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    expect(screen.getByText(/아직 글이 없습니다/)).toBeTruthy();
  });

  it('글_제목_링크를_클릭하면_상세로_라우팅한다', async () => {
    mockFetchSequence([
      {
        status: 200,
        body: listBody({ content: [samplePost(7, { title: 'hello world' })], totalElements: 1, totalPages: 1 }),
      },
    ]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('link', { name: /hello world/ }));
    });
    expect(currentPath).toBe('/posts/7');
  });

  it('글쓰기_버튼은_/posts/new_링크다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([{ status: 200, body: listBody() }]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    const link = screen.getByRole('link', { name: /글쓰기/ }) as HTMLAnchorElement;
    await act(async () => {
      fireEvent.click(link);
    });
    expect(currentPath).toBe('/posts/new');
  });

  it('비로그인_시_글쓰기_버튼은_미렌더이고_로그인_가입_링크가_보인다', async () => {
    mockFetchSequence([{ status: 200, body: listBody() }]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    expect(screen.queryByRole('link', { name: /글쓰기/ })).toBeNull();
    expect(screen.getByRole('link', { name: /^로그인$/ })).toBeTruthy();
    expect(screen.getByRole('link', { name: /^가입$/ })).toBeTruthy();
  });

  it('로그인_시_API키_링크와_로그아웃_버튼이_보인다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([{ status: 200, body: listBody() }]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    expect(screen.getByRole('link', { name: /API 키/ })).toBeTruthy();
    expect(screen.getByRole('button', { name: /로그아웃/ })).toBeTruthy();
    expect(screen.getByRole('link', { name: /글쓰기/ })).toBeTruthy();
  });

  it('로그아웃_클릭_시_tokenStore가_clear되고_로그인_링크로_바뀐다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([{ status: 200, body: listBody() }]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /로그아웃/ }));
    });
    expect(tokenStore.get()).toBeNull();
    expect(screen.queryByRole('button', { name: /로그아웃/ })).toBeNull();
    expect(screen.getByRole('link', { name: /^로그인$/ })).toBeTruthy();
  });

  it('다음_페이지_버튼_클릭_시_page1_을_요청한다', async () => {
    const fn = mockFetchSequence([
      { status: 200, body: listBody({ content: [samplePost(1)], totalElements: 25, totalPages: 3 }) },
      { status: 200, body: listBody({ content: [samplePost(11)], page: 1, totalElements: 25, totalPages: 3 }) },
    ]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /다음/ }));
    });
    await flush();
    expect(fn).toHaveBeenCalledTimes(2);
    const [url] = fn.mock.calls[1] as [string, RequestInit | undefined];
    expect(url).toBe('/api/posts?page=1&size=10');
  });

  it('마지막_페이지에서는_다음_버튼이_disabled_다', async () => {
    mockFetchSequence([
      { status: 200, body: listBody({ content: [samplePost(1)], totalElements: 5, totalPages: 1 }) },
    ]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    const next = screen.getByRole('button', { name: /다음/ }) as HTMLButtonElement;
    expect(next.disabled).toBe(true);
  });

  it('첫_페이지에서는_이전_버튼이_disabled_다', async () => {
    mockFetchSequence([
      { status: 200, body: listBody({ content: [samplePost(1)], totalElements: 25, totalPages: 3 }) },
    ]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    const prev = screen.getByRole('button', { name: /이전/ }) as HTMLButtonElement;
    expect(prev.disabled).toBe(true);
  });

  it('500_응답이면_에러_배너와_재시도_버튼이_뜬다', async () => {
    const fn = mockFetchSequence([
      { status: 500, body: { status: 500, error: 'Internal', message: '서버 오류', path: '/api/posts', timestamp: '' } },
      { status: 200, body: listBody({ content: [samplePost(1)], totalElements: 1, totalPages: 1 }) },
    ]);
    await act(async () => {
      renderListPage();
    });
    await flush();
    expect(screen.getByText(/서버 오류/)).toBeTruthy();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /재시도/ }));
    });
    await flush();
    expect(fn).toHaveBeenCalledTimes(2);
    expect(screen.getByText(/글 1/)).toBeTruthy();
  });
});

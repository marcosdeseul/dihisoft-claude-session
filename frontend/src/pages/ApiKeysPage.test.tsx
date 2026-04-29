import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import ApiKeysPage from './ApiKeysPage';
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

let currentPath: string | null = null;
function LocationSpy() {
  const loc = useLocation();
  currentPath = loc.pathname;
  return null;
}

function renderPage() {
  currentPath = null;
  return render(
    <MemoryRouter initialEntries={['/settings/api-keys']}>
      <Routes>
        <Route path="/settings/api-keys" element={<ApiKeysPage />} />
        <Route path="/login" element={<div data-testid="login-page">LOGIN</div>} />
        <Route path="/" element={<div data-testid="home">HOME</div>} />
      </Routes>
      <LocationSpy />
    </MemoryRouter>,
  );
}

interface MockResponse {
  status: number;
  body?: unknown;
}

function mockFetchSequence(responses: MockResponse[]): ReturnType<typeof vi.fn> {
  let i = 0;
  const fn = vi.fn(() => {
    const r = responses[Math.min(i, responses.length - 1)];
    i++;
    return Promise.resolve({
      ok: r.status >= 200 && r.status < 300,
      status: r.status,
      json: async () => r.body ?? {},
    } as Response);
  });
  globalThis.fetch = fn as unknown as typeof fetch;
  return fn;
}

const issuedBody = {
  id: 1,
  label: 'my-laptop',
  prefix: 'bk_AaBbCcDd',
  key: 'bk_AaBbCcDdeeFfGgHhIiJjKkLlMmNnOoPp',
  createdAt: '2026-04-28T12:00:00Z',
};

const summaryActive = {
  id: 1,
  label: 'my-laptop',
  prefix: 'bk_AaBbCcDd',
  lastUsedAt: null,
  createdAt: '2026-04-28T12:00:00Z',
  revokedAt: null,
};

describe('ApiKeysPage', () => {
  it('비로그인_상태에서_진입하면_login으로_리다이렉트된다', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId('login-page')).toBeTruthy());
    expect(currentPath).toBe('/login');
  });

  it('로그인_후_마운트_시_listApiKeys가_호출되고_빈_목록_메시지가_렌더된다', async () => {
    tokenStore.set('jwt-token');
    const fn = mockFetchSequence([{ status: 200, body: [] }]);
    await act(async () => {
      renderPage();
    });
    expect(fn).toHaveBeenCalled();
    const [url, init] = fn.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/me/api-keys');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer jwt-token');
    expect(await screen.findByText(/발급된 키가 없습니다/)).toBeTruthy();
  });

  it('label이_빈_문자열이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    tokenStore.set('jwt-token');
    const fn = mockFetchSequence([{ status: 200, body: [] }]);
    await act(async () => {
      renderPage();
    });
    fn.mockClear();
    fireEvent.change(screen.getByLabelText(/label/i), { target: { value: '   ' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /발급/ }));
    });
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('label은 필수입니다')).toBeTruthy();
  });

  it('정상_발급_시_평문_key_35자_bk_접두가_노출되고_복사_버튼이_보인다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([
      { status: 200, body: [] },
      { status: 201, body: issuedBody },
      { status: 200, body: [summaryActive] },
    ]);
    await act(async () => {
      renderPage();
    });
    fireEvent.change(screen.getByLabelText(/label/i), { target: { value: 'my-laptop' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /발급/ }));
    });
    const keyEl = await screen.findByText(issuedBody.key);
    expect(keyEl).toBeTruthy();
    expect(issuedBody.key.startsWith('bk_')).toBe(true);
    expect(issuedBody.key.length).toBe(35);
    expect(screen.getByRole('button', { name: /복사/ })).toBeTruthy();
    await waitFor(() => expect(screen.getByText('my-laptop')).toBeTruthy());
  });

  it('발급_평문_확인_버튼을_누르면_평문은_더_이상_보이지_않는다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([
      { status: 200, body: [] },
      { status: 201, body: issuedBody },
      { status: 200, body: [summaryActive] },
    ]);
    await act(async () => {
      renderPage();
    });
    fireEvent.change(screen.getByLabelText(/label/i), { target: { value: 'my-laptop' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /발급/ }));
    });
    await screen.findByText(issuedBody.key);
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /확인/ }));
    });
    expect(screen.queryByText(issuedBody.key)).toBeNull();
  });

  it('폐기_버튼을_누르면_revokeApiKey가_호출되고_목록이_갱신된다', async () => {
    tokenStore.set('jwt-token');
    const revokedSummary = { ...summaryActive, revokedAt: '2026-04-28T13:00:00Z' };
    const fn = mockFetchSequence([
      { status: 200, body: [summaryActive] },
      { status: 204, body: {} },
      { status: 200, body: [revokedSummary] },
    ]);
    vi.spyOn(globalThis, 'confirm').mockReturnValue(true);
    await act(async () => {
      renderPage();
    });
    await screen.findByText('my-laptop');
    fn.mockClear();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /폐기/ }));
    });
    const calls = fn.mock.calls.map((c) => c[0]);
    expect(calls.some((url) => url === '/api/me/api-keys/1')).toBe(true);
    await waitFor(() => expect(screen.getByText(/폐기/)).toBeTruthy());
  });

  it('401_응답이면_tokenStore가_초기화되고_login으로_리다이렉트된다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([{ status: 401, body: { status: 401, message: 'unauth' } }]);
    await act(async () => {
      renderPage();
    });
    await waitFor(() => expect(currentPath).toBe('/login'));
    expect(tokenStore.get()).toBeNull();
  });

  it('500_응답이면_에러_메시지가_노출되고_평문_key는_없다', async () => {
    tokenStore.set('jwt-token');
    mockFetchSequence([{ status: 500, body: { status: 500, message: '서버 오류' } }]);
    await act(async () => {
      renderPage();
    });
    expect(await screen.findByText(/오류|불러오/)).toBeTruthy();
    expect(screen.queryByText(/^bk_/)).toBeNull();
  });
});

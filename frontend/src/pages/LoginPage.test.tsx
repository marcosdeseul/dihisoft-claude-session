import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import LoginPage from './LoginPage';
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

function mockFetchResponse(response: { status: number; body: unknown }) {
  const fn = vi.fn(() =>
    Promise.resolve({
      ok: response.status >= 200 && response.status < 300,
      status: response.status,
      json: async () => response.body,
    } as Response),
  );
  globalThis.fetch = fn as unknown as typeof fetch;
  return fn;
}

let currentPath: string | null = null;
function LocationSpy() {
  const loc = useLocation();
  currentPath = loc.pathname;
  return null;
}

function renderLoginPage() {
  currentPath = null;
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<div data-testid="home">HOME</div>} />
        <Route path="*" element={<div>NOT FOUND</div>} />
      </Routes>
      <LocationSpy />
    </MemoryRouter>,
  );
}

function fill(username: string, password: string) {
  fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: username } });
  fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: password } });
}

async function clickSubmit(name: RegExp = /^로그인$/) {
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name }));
  });
}

const errorBody = (status: number, message: string) => ({
  status,
  error: 'Unauthorized',
  message,
  path: '/api/auth/login',
  timestamp: '2026-04-28T00:00:00Z',
});

describe('LoginPage', () => {
  it('폼_제출_시_login_API가_JSON으로_호출된다', async () => {
    const fn = mockFetchResponse({
      status: 200,
      body: { accessToken: 't.k.n', tokenType: 'Bearer', expiresIn: 3600 },
    });
    renderLoginPage();
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(fn).toHaveBeenCalledTimes(1);
    const [url, init] = fn.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/auth/login');
    expect(init.method).toBe('POST');
    const headers = init.headers as Record<string, string>;
    expect(headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(init.body as string)).toEqual({ username: 'marco', password: 'pw12345' });
  });

  it('200_응답_시_토큰을_저장하고_홈으로_네비게이트한다', async () => {
    mockFetchResponse({
      status: 200,
      body: { accessToken: 't.k.n', tokenType: 'Bearer', expiresIn: 3600 },
    });
    renderLoginPage();
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(tokenStore.get()).toBe('t.k.n');
    expect(currentPath).toBe('/');
    expect(screen.getByTestId('home')).toBeTruthy();
  });

  it('username이_빈_값이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderLoginPage();
    fill('', 'pw12345');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('username은 필수입니다')).toBeTruthy();
  });

  it('username이_공백만이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderLoginPage();
    fill('   ', 'pw12345');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('username은 필수입니다')).toBeTruthy();
  });

  it('password가_빈_값이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    renderLoginPage();
    fill('marco', '');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('password는 필수입니다')).toBeTruthy();
  });

  it('401_응답이면_고정_매핑_메시지를_렌더한다', async () => {
    mockFetchResponse({ status: 401, body: errorBody(401, 'Bad credentials') });
    renderLoginPage();
    fill('marco', 'wrong');
    await clickSubmit();
    expect(
      await screen.findByText('username과 password가 일치하지 않습니다'),
    ).toBeTruthy();
    expect(tokenStore.get()).toBeNull();
    expect(currentPath).toBe('/login');
  });

  it('500_응답이면_일반_오류_메시지를_렌더하고_홈으로_가지_않는다', async () => {
    mockFetchResponse({ status: 500, body: errorBody(500, '서버에 문제가 발생했습니다') });
    renderLoginPage();
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(await screen.findByText('서버에 문제가 발생했습니다')).toBeTruthy();
    expect(currentPath).toBe('/login');
  });

  it('네트워크_실패_시_fallback_메시지를_렌더하고_재제출_가능하다', async () => {
    globalThis.fetch = vi.fn(() => Promise.reject(new Error('network down'))) as unknown as typeof fetch;
    renderLoginPage();
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(await screen.findByText(/요청을 처리하지 못했습니다/)).toBeTruthy();
    const button = screen.getByRole('button', { name: /^로그인$/ }) as HTMLButtonElement;
    expect(button.disabled).toBe(false);
  });

  it('제출_중에는_버튼이_disabled_이다', async () => {
    let resolveFetch: (value: Response) => void = () => {};
    const pending = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });
    globalThis.fetch = vi.fn(() => pending) as unknown as typeof fetch;
    renderLoginPage();
    fill('marco', 'pw12345');
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^로그인$/ }));
    });
    const button = screen.getByRole('button', { name: /로그인 중/ }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    await act(async () => {
      resolveFetch({
        ok: true,
        status: 200,
        json: async () => ({ accessToken: 't', tokenType: 'Bearer', expiresIn: 3600 }),
      } as Response);
      await pending;
    });
  });

  it('401_후에도_username_입력은_보존된다', async () => {
    mockFetchResponse({ status: 401, body: errorBody(401, 'Bad credentials') });
    renderLoginPage();
    fill('marco', 'wrong');
    await clickSubmit();
    const userInput = screen.getByLabelText(/^username$/i) as HTMLInputElement;
    expect(userInput.value).toBe('marco');
  });
});

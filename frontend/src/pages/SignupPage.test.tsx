import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import SignupPage from './SignupPage';

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

beforeEach(() => {
  delete (globalThis as { fetch?: typeof fetch }).fetch;
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

function fill(username: string, password: string) {
  fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: username } });
  fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: password } });
}

async function clickSubmit(name: RegExp = /계정 만들기/) {
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name }));
  });
}

const errorBody = (message: string) => ({
  status: 400,
  error: 'Bad Request',
  message,
  path: '/api/auth/signup',
  timestamp: '2026-04-28T00:00:00Z',
});

describe('SignupPage', () => {
  it('폼_제출_시_signup_API가_JSON으로_호출된다', async () => {
    const fn = mockFetchResponse({ status: 201, body: { id: 1, username: 'marco' } });
    render(<SignupPage />);
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(fn).toHaveBeenCalledTimes(1);
    const [url, init] = fn.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/auth/signup');
    expect(init.method).toBe('POST');
    const headers = init.headers as Record<string, string>;
    expect(headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(init.body as string)).toEqual({ username: 'marco', password: 'pw12345' });
  });

  it('201_응답_시_가입_완료_메시지를_렌더한다', async () => {
    mockFetchResponse({ status: 201, body: { id: 1, username: 'marco' } });
    render(<SignupPage />);
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(await screen.findByText(/가입 완료/)).toBeTruthy();
  });

  it('username이_2자면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    render(<SignupPage />);
    fill('ab', 'pw12345');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('username은 3~20자여야 합니다')).toBeTruthy();
  });

  it('username이_21자면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    render(<SignupPage />);
    fill('a'.repeat(21), 'pw12345');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('username은 3~20자여야 합니다')).toBeTruthy();
  });

  it('username이_빈_값이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    render(<SignupPage />);
    fill('', 'pw12345');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('username은 필수입니다')).toBeTruthy();
  });

  it('password가_5자면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    render(<SignupPage />);
    fill('marco', '12345');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('password는 최소 6자여야 합니다')).toBeTruthy();
  });

  it('password가_빈_값이면_검증_메시지가_뜨고_API는_호출되지_않는다', async () => {
    const fn = vi.fn();
    globalThis.fetch = fn as unknown as typeof fetch;
    render(<SignupPage />);
    fill('marco', '');
    await clickSubmit();
    expect(fn).not.toHaveBeenCalled();
    expect(screen.getByText('password는 필수입니다')).toBeTruthy();
  });

  it('중복_username_400_응답이_오면_서버_메시지를_그대로_렌더한다', async () => {
    const dup = '이미 사용 중인 username입니다: marco';
    mockFetchResponse({ status: 400, body: errorBody(dup) });
    render(<SignupPage />);
    fill('marco', 'pw12345');
    await clickSubmit();
    expect(await screen.findByText(dup)).toBeTruthy();
  });

  it('실패_400_응답을_받아도_username_입력은_보존된다', async () => {
    mockFetchResponse({ status: 400, body: errorBody('username: username은 3~20자여야 합니다') });
    render(<SignupPage />);
    fill('marco', 'pw12345');
    await clickSubmit();
    const userInput = screen.getByLabelText(/^username$/i) as HTMLInputElement;
    expect(userInput.value).toBe('marco');
  });

  it('제출_중에는_버튼이_disabled_이다', async () => {
    let resolveFetch: (value: Response) => void = () => {};
    const pending = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });
    globalThis.fetch = vi.fn(() => pending) as unknown as typeof fetch;
    render(<SignupPage />);
    fill('marco', 'pw12345');
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /계정 만들기/ }));
    });
    const button = screen.getByRole('button', { name: /가입 중/ }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    await act(async () => {
      resolveFetch({
        ok: true,
        status: 201,
        json: async () => ({ id: 1, username: 'marco' }),
      } as Response);
      await pending;
    });
  });
});

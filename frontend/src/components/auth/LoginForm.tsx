import { FormEvent } from 'react';
import { authEditorialCss } from './authStyles';

export type LoginField = 'username' | 'password';

export interface LoginFormProps {
  username: string;
  password: string;
  onChange: (field: LoginField, value: string) => void;
  onSubmit: () => void;
  submitting?: boolean;
  error?: string | null;
  fieldErrors?: Partial<Record<LoginField, string>>;
}

export function LoginForm(props: LoginFormProps) {
  const { username, password, onChange, onSubmit, submitting, error, fieldErrors } = props;
  const userErr = fieldErrors?.username;
  const pwErr = fieldErrors?.password;
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!submitting) onSubmit();
  };
  return (
    <div className="auth-edit-page">
      <style>{authEditorialCss}</style>
      <div className="auth-edit-card">
        <p className="auth-edit-eyebrow">Board · Sign in</p>
        <h1 className="auth-edit-h1">로그인</h1>
        <p className="auth-edit-sub">다시 만나서 반가워요. 계정 정보를 입력해주세요.</p>
        {error && <div className="auth-edit-alert" role="alert">{error}</div>}
        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-edit-field">
            <label className="auth-edit-label" htmlFor="username">username</label>
            <div className="auth-edit-inputwrap">
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                className={`auth-edit-input${userErr ? ' is-error' : ''}`}
                value={username}
                onChange={(e) => onChange('username', e.target.value)}
              />
              {username && <span className={`auth-edit-mark ${userErr ? 'bad' : 'ok'}`}>{userErr ? '✗' : '✓'}</span>}
            </div>
            {userErr && <div className="auth-edit-fielderr">{userErr}</div>}
          </div>
          <div className="auth-edit-field">
            <label className="auth-edit-label" htmlFor="password">password</label>
            <div className="auth-edit-inputwrap">
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                className={`auth-edit-input${pwErr ? ' is-error' : ''}`}
                value={password}
                onChange={(e) => onChange('password', e.target.value)}
              />
              {password && <span className={`auth-edit-mark ${pwErr ? 'bad' : 'ok'}`}>{pwErr ? '✗' : '✓'}</span>}
            </div>
            {pwErr && <div className="auth-edit-fielderr">{pwErr}</div>}
          </div>
          <button type="submit" className="auth-edit-cta" disabled={submitting}>
            {submitting ? '로그인 중…' : '로그인'}
          </button>
        </form>
        <div className="auth-edit-foot">
          계정이 없나요? <a href="/signup">회원가입 →</a>
        </div>
      </div>
    </div>
  );
}

import { FormEvent } from 'react';
import { authEditorialCss } from '../auth/authStyles';

export type SignupField = 'username' | 'password';

export interface SignupFormProps {
  username: string;
  password: string;
  onChange: (field: SignupField, value: string) => void;
  onSubmit: () => void;
  submitting?: boolean;
  error?: string | null;
  success?: string | null;
  fieldErrors?: Partial<Record<SignupField, string>>;
}

export function SignupForm(props: SignupFormProps) {
  const { username, password, onChange, onSubmit, submitting, error, success, fieldErrors } = props;
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
        <p className="auth-edit-eyebrow">Board · Sign up</p>
        <h1 className="auth-edit-h1">회원가입</h1>
        <p className="auth-edit-sub">계정을 만들어 글을 쓰고 토론에 참여하세요.</p>
        {success && <div className="auth-edit-success" role="status">{success}</div>}
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
                autoComplete="new-password"
                className={`auth-edit-input${pwErr ? ' is-error' : ''}`}
                value={password}
                onChange={(e) => onChange('password', e.target.value)}
              />
              {password && <span className={`auth-edit-mark ${pwErr ? 'bad' : 'ok'}`}>{pwErr ? '✗' : '✓'}</span>}
            </div>
            {pwErr && <div className="auth-edit-fielderr">{pwErr}</div>}
          </div>
          <button type="submit" className="auth-edit-cta" disabled={submitting}>
            {submitting ? '가입 중…' : '계정 만들기'}
          </button>
        </form>
        <div className="auth-edit-foot">
          이미 계정이 있나요? <a href="/login">로그인 →</a>
        </div>
      </div>
    </div>
  );
}

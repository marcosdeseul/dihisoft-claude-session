import { FormEvent } from 'react';

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

const editorialCss = `
.sf-edit-page { min-height: 100vh; background: #f6f4ef; display: grid; place-items: center; padding: 48px 16px; font-family: 'Inter', system-ui, sans-serif; color: #1a1a1a; }
.sf-edit-card { width: 100%; max-width: 440px; background: #fff; border: 1px solid #ece8df; border-radius: 14px; padding: 40px 36px 32px; box-shadow: 0 4px 24px rgba(20,20,30,0.05); }
.sf-edit-eyebrow { font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; color: #8a7f6f; margin: 0 0 12px; }
.sf-edit-h1 { font-family: 'Source Serif Pro', Georgia, 'Times New Roman', serif; font-size: 30px; line-height: 1.15; margin: 0 0 8px; font-weight: 600; }
.sf-edit-sub { color: #5b5247; font-size: 15px; margin: 0 0 28px; }
.sf-edit-field { margin-bottom: 18px; }
.sf-edit-label { display: block; font-size: 13px; color: #4a4036; margin-bottom: 6px; font-weight: 500; }
.sf-edit-inputwrap { position: relative; }
.sf-edit-input { width: 100%; box-sizing: border-box; padding: 12px 36px 12px 14px; border: 1px solid #ddd5c5; border-radius: 8px; font-size: 15px; background: #fafaf6; transition: border-color 120ms, box-shadow 120ms; outline: none; }
.sf-edit-input:focus { border-color: #1a1a1a; box-shadow: 0 0 0 3px rgba(26,26,26,0.08); background: #fff; }
.sf-edit-input.is-error { border-color: #b6324a; background: #fdf3f4; }
.sf-edit-mark { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); font-size: 14px; }
.sf-edit-mark.ok { color: #2c7a4d; }
.sf-edit-mark.bad { color: #b6324a; }
.sf-edit-fielderr { color: #b6324a; font-size: 12.5px; margin-top: 6px; }
.sf-edit-alert { background: #fdf3f4; border: 1px solid #f1ccd2; color: #8c2237; padding: 12px 14px; border-radius: 8px; font-size: 14px; margin-bottom: 18px; }
.sf-edit-success { background: #eef7f1; border: 1px solid #c8e3d2; color: #1f5b3a; padding: 12px 14px; border-radius: 8px; font-size: 14px; margin-bottom: 18px; }
.sf-edit-cta { width: 100%; padding: 13px 16px; background: #1a1a1a; color: #fff; border: 0; border-radius: 8px; font-size: 15px; font-weight: 600; letter-spacing: 0.02em; cursor: pointer; transition: background 120ms; }
.sf-edit-cta:hover:not(:disabled) { background: #000; }
.sf-edit-cta:disabled { background: #6e6358; cursor: not-allowed; }
.sf-edit-foot { margin-top: 18px; text-align: center; font-size: 13.5px; color: #5b5247; }
.sf-edit-foot a { color: #1a1a1a; font-weight: 500; text-decoration: none; }
.sf-edit-foot a:hover { text-decoration: underline; }
`;

export function SignupForm(props: SignupFormProps) {
  const { username, password, onChange, onSubmit, submitting, error, success, fieldErrors } = props;
  const userErr = fieldErrors?.username;
  const pwErr = fieldErrors?.password;
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!submitting) onSubmit();
  };
  return (
    <div className="sf-edit-page">
      <style>{editorialCss}</style>
      <div className="sf-edit-card">
        <p className="sf-edit-eyebrow">Board · Sign up</p>
        <h1 className="sf-edit-h1">회원가입</h1>
        <p className="sf-edit-sub">계정을 만들어 글을 쓰고 토론에 참여하세요.</p>
        {success && <div className="sf-edit-success" role="status">{success}</div>}
        {error && <div className="sf-edit-alert" role="alert">{error}</div>}
        <form onSubmit={handleSubmit} noValidate>
          <div className="sf-edit-field">
            <label className="sf-edit-label" htmlFor="username">username</label>
            <div className="sf-edit-inputwrap">
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                className={`sf-edit-input${userErr ? ' is-error' : ''}`}
                value={username}
                onChange={(e) => onChange('username', e.target.value)}
              />
              {username && <span className={`sf-edit-mark ${userErr ? 'bad' : 'ok'}`}>{userErr ? '✗' : '✓'}</span>}
            </div>
            {userErr && <div className="sf-edit-fielderr">{userErr}</div>}
          </div>
          <div className="sf-edit-field">
            <label className="sf-edit-label" htmlFor="password">password</label>
            <div className="sf-edit-inputwrap">
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="new-password"
                className={`sf-edit-input${pwErr ? ' is-error' : ''}`}
                value={password}
                onChange={(e) => onChange('password', e.target.value)}
              />
              {password && <span className={`sf-edit-mark ${pwErr ? 'bad' : 'ok'}`}>{pwErr ? '✗' : '✓'}</span>}
            </div>
            {pwErr && <div className="sf-edit-fielderr">{pwErr}</div>}
          </div>
          <button type="submit" className="sf-edit-cta" disabled={submitting}>
            {submitting ? '가입 중…' : '계정 만들기'}
          </button>
        </form>
        <div className="sf-edit-foot">
          이미 계정이 있나요? <a href="/login">로그인 →</a>
        </div>
      </div>
    </div>
  );
}

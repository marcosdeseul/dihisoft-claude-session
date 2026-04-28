import { useState, type FormEvent } from 'react';
import {
  ApiKeyError,
  issueApiKey,
  type IssuedApiKeyResponse,
} from '../../api/apiKeys';

export interface ApiKeyFormProps {
  onIssued: () => void;
  onAuthError: () => void;
}

export function ApiKeyForm({ onIssued, onAuthError }: ApiKeyFormProps) {
  const [label, setLabel] = useState('');
  const [labelError, setLabelError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [issued, setIssued] = useState<IssuedApiKeyResponse | null>(null);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLabelError(null);
    setSubmitError(null);
    if (label.trim().length === 0) {
      setLabelError('label은 필수입니다');
      return;
    }
    setSubmitting(true);
    try {
      const result = await issueApiKey(label.trim());
      setIssued(result);
      setLabel('');
      onIssued();
    } catch (err) {
      if (err instanceof ApiKeyError && err.status === 401) onAuthError();
      else if (err instanceof ApiKeyError) setSubmitError(err.message);
      else setSubmitError('요청을 처리하지 못했습니다');
    } finally {
      setSubmitting(false);
    }
  };

  const onCopy = () => {
    if (issued && typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(issued.key).catch(() => {});
    }
  };

  return (
    <form onSubmit={onSubmit} className="auth-edit-card">
      <p className="auth-edit-eyebrow">Settings · API Keys</p>
      <h2 className="auth-edit-h1">새 키 발급</h2>
      <p className="auth-edit-sub">키는 발급 직후 1회만 노출됩니다. 안전한 곳에 보관하세요.</p>
      {submitError && (
        <div className="auth-edit-alert" role="alert">
          {submitError}
        </div>
      )}
      <div className="auth-edit-field">
        <label htmlFor="apikey-label" className="auth-edit-label">
          label
        </label>
        <input
          id="apikey-label"
          type="text"
          className={'auth-edit-input' + (labelError ? ' is-error' : '')}
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          disabled={submitting || issued !== null}
        />
        {labelError && <div className="auth-edit-fielderr">{labelError}</div>}
      </div>
      {issued ? (
        <div className="auth-edit-success" role="status">
          <p style={{ margin: '0 0 8px', fontWeight: 600 }}>발급된 키 (다시 볼 수 없음)</p>
          <code
            style={{
              display: 'block',
              wordBreak: 'break-all',
              background: '#fff',
              padding: 10,
              borderRadius: 6,
              marginBottom: 10,
              fontFamily: 'ui-monospace, Menlo, monospace',
            }}
          >
            {issued.key}
          </code>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="button" className="auth-edit-cta" style={{ flex: 1 }} onClick={onCopy}>
              복사
            </button>
            <button
              type="button"
              className="auth-edit-cta"
              style={{ flex: 1, background: '#5b5247' }}
              onClick={() => setIssued(null)}
            >
              확인
            </button>
          </div>
        </div>
      ) : (
        <button type="submit" className="auth-edit-cta" disabled={submitting}>
          {submitting ? '발급 중…' : '발급'}
        </button>
      )}
    </form>
  );
}

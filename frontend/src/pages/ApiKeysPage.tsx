import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import {
  ApiKeyError,
  listApiKeys,
  revokeApiKey,
  type ApiKeySummaryResponse,
} from '../api/apiKeys';
import * as tokenStore from '../auth/tokenStore';
import { ApiKeyForm } from '../components/apiKeys/ApiKeyForm';
import { ApiKeyList } from '../components/apiKeys/ApiKeyList';
import { authEditorialCss } from '../components/auth/authStyles';

const FALLBACK_ERROR = '키 목록을 불러오지 못했습니다';

export default function ApiKeysPage() {
  const navigate = useNavigate();
  const authed = tokenStore.get() !== null;
  const [keys, setKeys] = useState<ApiKeySummaryResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAuthError = useCallback(() => {
    tokenStore.clear();
    navigate('/login');
  }, [navigate]);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    listApiKeys()
      .then((res) => setKeys(res))
      .catch((e) => {
        if (e instanceof ApiKeyError && e.status === 401) handleAuthError();
        else if (e instanceof ApiKeyError) setError(e.message || FALLBACK_ERROR);
        else setError(FALLBACK_ERROR);
      })
      .finally(() => setLoading(false));
  }, [handleAuthError]);

  useEffect(() => {
    if (authed) reload();
  }, [authed, reload]);

  if (!authed) return <Navigate to="/login" replace />;

  const onRevoke = (id: number) => {
    revokeApiKey(id)
      .then(() => reload())
      .catch((e) => {
        if (e instanceof ApiKeyError && e.status === 401) handleAuthError();
        else if (e instanceof ApiKeyError) setError(e.message || FALLBACK_ERROR);
        else setError(FALLBACK_ERROR);
      });
  };

  return (
    <div className="auth-edit-page">
      <style>{authEditorialCss}</style>
      <div style={{ width: '100%', maxWidth: 720 }}>
        <p style={{ marginBottom: 16 }}>
          <Link to="/">← 게시판으로</Link>
        </p>
        <ApiKeyForm onIssued={reload} onAuthError={handleAuthError} />
        <div style={{ marginTop: 32 }}>
          <ApiKeyList keys={keys} loading={loading} error={error} onRevoke={onRevoke} />
        </div>
      </div>
    </div>
  );
}

import type { ApiKeySummaryResponse } from '../../api/apiKeys';

export interface ApiKeyListProps {
  keys: ApiKeySummaryResponse[];
  loading: boolean;
  error: string | null;
  onRevoke: (id: number) => void;
}

function formatDateTime(iso: string | null): string {
  if (!iso) return '—';
  return iso.slice(0, 19).replace('T', ' ');
}

export function ApiKeyList({ keys, loading, error, onRevoke }: ApiKeyListProps) {
  if (loading) return <p>불러오는 중…</p>;
  if (error)
    return (
      <div className="auth-edit-alert" role="alert">
        {error}
      </div>
    );
  if (keys.length === 0) return <p>발급된 키가 없습니다.</p>;

  const handleRevoke = (id: number, label: string) => {
    if (window.confirm(`키 "${label}"을(를) 폐기하시겠습니까?`)) {
      onRevoke(id);
    }
  };

  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
      <thead>
        <tr style={{ borderBottom: '1px solid #ece8df', textAlign: 'left' }}>
          <th style={{ padding: '8px 6px' }}>label</th>
          <th style={{ padding: '8px 6px' }}>prefix</th>
          <th style={{ padding: '8px 6px' }}>발급일</th>
          <th style={{ padding: '8px 6px' }}>마지막 사용</th>
          <th style={{ padding: '8px 6px' }}>상태</th>
          <th style={{ padding: '8px 6px' }}></th>
        </tr>
      </thead>
      <tbody>
        {keys.map((k) => {
          const revoked = k.revokedAt !== null;
          return (
            <tr key={k.id} style={{ borderBottom: '1px solid #f3eee2' }}>
              <td style={{ padding: '8px 6px' }}>{k.label}</td>
              <td style={{ padding: '8px 6px' }}>
                <code>{k.prefix}</code>
              </td>
              <td style={{ padding: '8px 6px' }}>{formatDateTime(k.createdAt)}</td>
              <td style={{ padding: '8px 6px' }}>{formatDateTime(k.lastUsedAt)}</td>
              <td style={{ padding: '8px 6px' }}>
                {revoked ? `폐기 ${formatDateTime(k.revokedAt)}` : '활성'}
              </td>
              <td style={{ padding: '8px 6px', textAlign: 'right' }}>
                {!revoked && (
                  <button type="button" onClick={() => handleRevoke(k.id, k.label)}>
                    폐기
                  </button>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

import { Link } from 'react-router-dom';
import type { PostResponse } from '../../api/posts';
import { postEditorialCss } from './postStyles';

export interface PostListProps {
  posts: PostResponse[];
  loading?: boolean;
  error?: string | null;
  page: number;
  totalPages: number;
  onPrev: () => void;
  onNext: () => void;
  onRetry?: () => void;
}

function formatDate(iso: string): string {
  return iso.slice(0, 10);
}

export function PostList(props: PostListProps) {
  const { posts, loading, error, page, totalPages, onPrev, onNext, onRetry } = props;
  const hasPrev = page > 0;
  const hasNext = page < Math.max(totalPages - 1, 0);
  return (
    <div className="post-page">
      <style>{postEditorialCss}</style>
      <div className="post-shell">
        <p className="post-eyebrow">Board · Posts</p>
        <h1 className="post-h1">게시판</h1>
        <div className="post-toolbar">
          <div className="post-pagination-info">
            {totalPages > 0 ? `${page + 1} / ${totalPages}` : ' '}
          </div>
          <div className="post-toolbar-right">
            <Link to="/posts/new" className="post-btn">글쓰기</Link>
          </div>
        </div>
        {error && (
          <div className="post-alert" role="alert">
            <span>{error}</span>
            {onRetry && (
              <>
                {' '}
                <button type="button" className="post-btn is-ghost" onClick={onRetry}>재시도</button>
              </>
            )}
          </div>
        )}
        {loading && !error && <div className="post-empty">불러오는 중…</div>}
        {!loading && !error && posts.length === 0 && (
          <div className="post-empty">아직 글이 없습니다. 첫 글을 작성해보세요.</div>
        )}
        {!error && posts.length > 0 && (
          <ul className="post-list">
            {posts.map((p) => (
              <li key={p.id} className="post-list-item">
                <Link to={`/posts/${p.id}`} className="post-list-link">
                  <p className="post-list-title">{p.title}</p>
                  <p className="post-list-meta">{p.authorUsername} · {formatDate(p.createdAt)}</p>
                </Link>
              </li>
            ))}
          </ul>
        )}
        <div className="post-pagination">
          <button
            type="button"
            className="post-btn is-ghost"
            disabled={!hasPrev || loading}
            onClick={onPrev}
          >
            이전
          </button>
          <button
            type="button"
            className="post-btn is-ghost"
            disabled={!hasNext || loading}
            onClick={onNext}
          >
            다음
          </button>
        </div>
      </div>
    </div>
  );
}

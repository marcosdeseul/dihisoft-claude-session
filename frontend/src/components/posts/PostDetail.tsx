import { Link } from 'react-router-dom';
import type { PostResponse } from '../../api/posts';
import { postEditorialCss } from './postStyles';

export interface PostDetailProps {
  post: PostResponse;
  canEdit: boolean;
  deleteError?: string | null;
  deleting?: boolean;
  onDelete: () => void;
}

function formatDateTime(iso: string): string {
  return iso.replace('T', ' ').slice(0, 16);
}

export function PostDetail(props: PostDetailProps) {
  const { post, canEdit, deleteError, deleting, onDelete } = props;
  return (
    <div className="post-page">
      <style>{postEditorialCss}</style>
      <div className="post-shell">
        <p className="post-eyebrow">Board · Post</p>
        <h1 className="post-h1">{post.title}</h1>
        <p className="post-detail-meta">
          {post.authorUsername} · {formatDateTime(post.createdAt)}
          {post.updatedAt !== post.createdAt && ` · 수정됨 ${formatDateTime(post.updatedAt)}`}
        </p>
        {deleteError && <div className="post-alert" role="alert">{deleteError}</div>}
        <div className="post-toolbar">
          <Link to="/" className="post-btn is-ghost">← 목록</Link>
          {canEdit && (
            <div className="post-toolbar-right">
              <Link to={`/posts/${post.id}/edit`} className="post-btn is-ghost">수정</Link>
              <button
                type="button"
                className="post-btn is-danger"
                onClick={onDelete}
                disabled={deleting}
              >
                {deleting ? '삭제 중…' : '삭제'}
              </button>
            </div>
          )}
        </div>
        <div className="post-detail-body">{post.content}</div>
      </div>
    </div>
  );
}

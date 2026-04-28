import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { deletePost, getPost, PostError, type PostResponse } from '../api/posts';
import * as tokenStore from '../auth/tokenStore';
import { PostDetail } from '../components/posts/PostDetail';

const NOT_FOUND_MESSAGE = '게시글을 찾을 수 없습니다';
const FORBIDDEN_MESSAGE = '본인의 글만 수정/삭제할 수 있습니다';
const FALLBACK_ERROR = '요청을 처리하지 못했습니다';

export default function PostDetailPage() {
  const params = useParams<{ id: string }>();
  const navigate = useNavigate();
  const id = Number(params.id);
  const [post, setPost] = useState<PostResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [authed] = useState<boolean>(() => tokenStore.get() !== null);

  useEffect(() => {
    let cancelled = false;
    getPost(id)
      .then((res) => {
        if (!cancelled) setPost(res);
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof PostError && e.status === 404) setLoadError(NOT_FOUND_MESSAGE);
        else if (e instanceof PostError) setLoadError(e.message || FALLBACK_ERROR);
        else setLoadError(FALLBACK_ERROR);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const onDelete = () => {
    if (!window.confirm('이 글을 삭제하시겠습니까?')) return;
    setDeleting(true);
    setDeleteError(null);
    deletePost(id)
      .then(() => navigate('/'))
      .catch((e) => {
        if (e instanceof PostError && e.status === 401) {
          tokenStore.clear();
          navigate('/login');
          return;
        }
        if (e instanceof PostError && e.status === 403) {
          setDeleteError(FORBIDDEN_MESSAGE);
        } else if (e instanceof PostError) {
          setDeleteError(e.message || FALLBACK_ERROR);
        } else {
          setDeleteError(FALLBACK_ERROR);
        }
      })
      .finally(() => setDeleting(false));
  };

  if (loadError) {
    return (
      <div className="post-page">
        <div className="post-shell">
          <div className="post-alert" role="alert">{loadError}</div>
        </div>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="post-page">
        <div className="post-shell">
          <div className="post-empty">불러오는 중…</div>
        </div>
      </div>
    );
  }

  return (
    <PostDetail
      post={post}
      canEdit={authed}
      deleteError={deleteError}
      deleting={deleting}
      onDelete={onDelete}
    />
  );
}

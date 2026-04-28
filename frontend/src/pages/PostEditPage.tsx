import { useEffect, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import { getPost, PostError, updatePost } from '../api/posts';
import * as tokenStore from '../auth/tokenStore';
import { PostForm, type PostFormField } from '../components/posts/PostForm';

const NOT_FOUND_MESSAGE = '게시글을 찾을 수 없습니다';
const FORBIDDEN_MESSAGE = '본인의 글만 수정/삭제할 수 있습니다';
const FALLBACK_ERROR = '요청을 처리하지 못했습니다';

function validate(title: string, content: string): Partial<Record<PostFormField, string>> {
  const errors: Partial<Record<PostFormField, string>> = {};
  if (title.trim().length === 0) errors.title = 'title은 필수입니다';
  else if (title.length > 100) errors.title = 'title은 최대 100자입니다';
  if (content.length === 0) errors.content = 'content는 필수입니다';
  return errors;
}

export default function PostEditPage() {
  const params = useParams<{ id: string }>();
  const navigate = useNavigate();
  const id = Number(params.id);
  const isAuthed = tokenStore.get() !== null;
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<PostFormField, string>>>({});

  useEffect(() => {
    if (!isAuthed) return undefined;
    let cancelled = false;
    getPost(id)
      .then((res) => {
        if (cancelled) return;
        setTitle(res.title);
        setContent(res.content);
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
  }, [id, isAuthed]);

  if (!isAuthed) {
    return <Navigate to="/login" replace />;
  }

  const onChange = (field: PostFormField, value: string) => {
    if (field === 'title') setTitle(value);
    else setContent(value);
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
    if (error) setError(null);
  };

  const onSubmit = async () => {
    const errors = validate(title, content);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});
    setError(null);
    setSubmitting(true);
    try {
      await updatePost(id, { title: title.trim(), content });
      navigate(`/posts/${id}`);
    } catch (e) {
      if (e instanceof PostError && e.status === 401) {
        tokenStore.clear();
        navigate('/login');
        return;
      }
      if (e instanceof PostError && e.status === 403) {
        setError(FORBIDDEN_MESSAGE);
      } else if (e instanceof PostError) {
        setError(e.message || FALLBACK_ERROR);
      } else {
        setError(FALLBACK_ERROR);
      }
    } finally {
      setSubmitting(false);
    }
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

  return (
    <PostForm
      mode="update"
      title={title}
      content={content}
      onChange={onChange}
      onSubmit={onSubmit}
      submitting={submitting}
      error={error}
      fieldErrors={fieldErrors}
    />
  );
}

import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { createPost, PostError } from '../api/posts';
import * as tokenStore from '../auth/tokenStore';
import { PostForm, type PostFormField } from '../components/posts/PostForm';

const FALLBACK_ERROR = '요청을 처리하지 못했습니다';

function validate(title: string, content: string): Partial<Record<PostFormField, string>> {
  const errors: Partial<Record<PostFormField, string>> = {};
  if (title.trim().length === 0) errors.title = 'title은 필수입니다';
  else if (title.length > 100) errors.title = 'title은 최대 100자입니다';
  if (content.length === 0) errors.content = 'content는 필수입니다';
  return errors;
}

export default function PostNewPage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<PostFormField, string>>>({});

  if (tokenStore.get() === null) {
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
      const created = await createPost({ title: title.trim(), content });
      navigate(`/posts/${created.id}`);
    } catch (e) {
      if (e instanceof PostError && e.status === 401) {
        tokenStore.clear();
        navigate('/login');
        return;
      }
      if (e instanceof PostError) setError(e.message || FALLBACK_ERROR);
      else setError(FALLBACK_ERROR);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PostForm
      mode="create"
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

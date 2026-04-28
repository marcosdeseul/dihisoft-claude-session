import { FormEvent } from 'react';
import { authEditorialCss } from '../auth/authStyles';
import { postEditorialCss } from './postStyles';

export type PostFormField = 'title' | 'content';
export type PostFormMode = 'create' | 'update';

export interface PostFormProps {
  mode: PostFormMode;
  title: string;
  content: string;
  onChange: (field: PostFormField, value: string) => void;
  onSubmit: () => void;
  submitting?: boolean;
  error?: string | null;
  fieldErrors?: Partial<Record<PostFormField, string>>;
}

const COPY: Record<PostFormMode, { eyebrow: string; heading: string; sub: string; cta: string; ctaPending: string }> = {
  create: {
    eyebrow: 'Board · New',
    heading: '글쓰기',
    sub: '제목과 내용을 입력해 게시판에 글을 남겨보세요.',
    cta: '등록',
    ctaPending: '등록 중…',
  },
  update: {
    eyebrow: 'Board · Edit',
    heading: '글 수정',
    sub: '내용을 다듬고 다시 저장하세요.',
    cta: '수정',
    ctaPending: '수정 중…',
  },
};

export function PostForm(props: PostFormProps) {
  const { mode, title, content, onChange, onSubmit, submitting, error, fieldErrors } = props;
  const copy = COPY[mode];
  const titleErr = fieldErrors?.title;
  const contentErr = fieldErrors?.content;
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!submitting) onSubmit();
  };
  return (
    <div className="auth-edit-page">
      <style>{authEditorialCss}</style>
      <style>{postEditorialCss}</style>
      <div className="auth-edit-card post-form-card">
        <p className="auth-edit-eyebrow">{copy.eyebrow}</p>
        <h1 className="auth-edit-h1">{copy.heading}</h1>
        <p className="auth-edit-sub">{copy.sub}</p>
        {error && <div className="auth-edit-alert" role="alert">{error}</div>}
        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-edit-field">
            <label className="auth-edit-label" htmlFor="title">title</label>
            <div className="auth-edit-inputwrap">
              <input
                id="title"
                name="title"
                type="text"
                className={`auth-edit-input${titleErr ? ' is-error' : ''}`}
                value={title}
                onChange={(e) => onChange('title', e.target.value)}
                maxLength={120}
              />
            </div>
            {titleErr && <div className="auth-edit-fielderr">{titleErr}</div>}
          </div>
          <div className="auth-edit-field">
            <label className="auth-edit-label" htmlFor="content">content</label>
            <textarea
              id="content"
              name="content"
              className={`post-textarea${contentErr ? ' is-error' : ''}`}
              value={content}
              onChange={(e) => onChange('content', e.target.value)}
            />
            {contentErr && <div className="auth-edit-fielderr">{contentErr}</div>}
          </div>
          <button type="submit" className="auth-edit-cta" disabled={submitting}>
            {submitting ? copy.ctaPending : copy.cta}
          </button>
        </form>
      </div>
    </div>
  );
}

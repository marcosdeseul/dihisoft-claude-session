import { useState } from 'react';
import { signup, SignupError } from '../api/auth';
import { SignupField, SignupForm } from '../components/signup/SignupForm';

const SUCCESS_MESSAGE = '가입 완료! 이어서 로그인하세요.';

function validate(username: string, password: string): Partial<Record<SignupField, string>> {
  const errors: Partial<Record<SignupField, string>> = {};
  const trimmedUsername = username.trim();
  if (trimmedUsername.length === 0) {
    errors.username = 'username은 필수입니다';
  } else if (trimmedUsername.length < 3 || trimmedUsername.length > 20) {
    errors.username = 'username은 3~20자여야 합니다';
  }
  if (password.length === 0) {
    errors.password = 'password는 필수입니다';
  } else if (password.length < 6) {
    errors.password = 'password는 최소 6자여야 합니다';
  }
  return errors;
}

export default function SignupPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<SignupField, string>>>({});

  const onChange = (field: SignupField, value: string) => {
    if (field === 'username') setUsername(value);
    else setPassword(value);
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
    if (error) setError(null);
  };

  const onSubmit = async () => {
    const errors = validate(username, password);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});
    setError(null);
    setSuccess(null);
    setSubmitting(true);
    try {
      await signup({ username: username.trim(), password });
      setSuccess(SUCCESS_MESSAGE);
    } catch (e) {
      const message =
        e instanceof SignupError
          ? e.message
          : (e as Error)?.message || '요청을 처리하지 못했습니다';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SignupForm
      username={username}
      password={password}
      onChange={onChange}
      onSubmit={onSubmit}
      submitting={submitting}
      error={error}
      success={success}
      fieldErrors={fieldErrors}
    />
  );
}

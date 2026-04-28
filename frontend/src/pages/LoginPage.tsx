import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, LoginError } from '../api/auth';
import * as tokenStore from '../auth/tokenStore';
import { LoginField, LoginForm } from '../components/auth/LoginForm';

const LOGIN_FAIL_MESSAGE = 'username과 password가 일치하지 않습니다';
const FALLBACK_MESSAGE = '요청을 처리하지 못했습니다';

function validate(username: string, password: string): Partial<Record<LoginField, string>> {
  const errors: Partial<Record<LoginField, string>> = {};
  if (username.trim().length === 0) {
    errors.username = 'username은 필수입니다';
  }
  if (password.length === 0) {
    errors.password = 'password는 필수입니다';
  }
  return errors;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<LoginField, string>>>({});

  const onChange = (field: LoginField, value: string) => {
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
    setSubmitting(true);
    try {
      const token = await login({ username: username.trim(), password });
      tokenStore.set(token.accessToken);
      navigate('/');
    } catch (e) {
      if (e instanceof LoginError && e.status === 401) {
        setError(LOGIN_FAIL_MESSAGE);
      } else if (e instanceof LoginError) {
        setError(e.message || FALLBACK_MESSAGE);
      } else {
        setError(FALLBACK_MESSAGE);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <LoginForm
      username={username}
      password={password}
      onChange={onChange}
      onSubmit={onSubmit}
      submitting={submitting}
      error={error}
      fieldErrors={fieldErrors}
    />
  );
}

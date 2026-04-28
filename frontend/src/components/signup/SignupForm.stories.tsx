import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { SignupForm, SignupFormProps } from './SignupForm';

interface HostProps extends Omit<SignupFormProps, 'username' | 'password' | 'onChange' | 'onSubmit'> {
  initialUsername?: string;
  initialPassword?: string;
}

function Host({ initialUsername = '', initialPassword = '', ...rest }: HostProps) {
  const [username, setUsername] = useState(initialUsername);
  const [password, setPassword] = useState(initialPassword);
  const onChange: SignupFormProps['onChange'] = (field, value) => {
    if (field === 'username') setUsername(value);
    else setPassword(value);
  };
  return (
    <SignupForm
      username={username}
      password={password}
      onChange={onChange}
      onSubmit={() => {
        /* no-op in Storybook */
      }}
      {...rest}
    />
  );
}

const meta: Meta<typeof Host> = {
  title: 'Signup/SignupForm',
  component: Host,
  parameters: { layout: 'fullscreen' },
};
export default meta;

type Story = StoryObj<typeof Host>;

export const Idle: Story = { args: {} };

export const Submitting: Story = {
  args: { initialUsername: 'marco', initialPassword: 'pw12345', submitting: true },
};

export const DuplicateError: Story = {
  args: {
    initialUsername: 'marco',
    initialPassword: 'pw12345',
    error: '이미 사용 중인 username입니다: marco',
  },
};

export const FieldError: Story = {
  args: {
    initialUsername: 'ab',
    initialPassword: '123',
    fieldErrors: {
      username: 'username은 3~20자여야 합니다',
      password: 'password는 최소 6자여야 합니다',
    },
  },
};

export const Success: Story = {
  args: {
    initialUsername: 'marco',
    initialPassword: 'pw12345',
    success: '가입 완료! 이어서 로그인하세요.',
  },
};

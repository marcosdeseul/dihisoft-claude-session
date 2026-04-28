import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { PostForm, PostFormProps } from './PostForm';

interface HostProps extends Omit<PostFormProps, 'title' | 'content' | 'onChange' | 'onSubmit'> {
  initialTitle?: string;
  initialContent?: string;
}

function Host({ initialTitle = '', initialContent = '', ...rest }: HostProps) {
  const [title, setTitle] = useState(initialTitle);
  const [content, setContent] = useState(initialContent);
  const onChange: PostFormProps['onChange'] = (field, value) => {
    if (field === 'title') setTitle(value);
    else setContent(value);
  };
  return (
    <PostForm
      title={title}
      content={content}
      onChange={onChange}
      onSubmit={() => {
        /* no-op in Storybook */
      }}
      {...rest}
    />
  );
}

const meta: Meta<typeof Host> = {
  title: 'Posts/PostForm',
  component: Host,
  parameters: { layout: 'fullscreen' },
};
export default meta;

type Story = StoryObj<typeof Host>;

export const CreateIdle: Story = { args: { mode: 'create' } };

export const CreateSubmitting: Story = {
  args: {
    mode: 'create',
    initialTitle: '오늘의 회고',
    initialContent: '하루를 돌아본다.\n\n잘한 일과 아쉬운 점을 정리하자.',
    submitting: true,
  },
};

export const CreateFieldError: Story = {
  args: {
    mode: 'create',
    initialTitle: '',
    initialContent: '',
    fieldErrors: {
      title: 'title은 필수입니다',
      content: 'content는 필수입니다',
    },
  },
};

export const CreateServerError: Story = {
  args: {
    mode: 'create',
    initialTitle: '제목',
    initialContent: '본문',
    error: 'title: title은 최대 100자입니다',
  },
};

export const UpdateIdle: Story = {
  args: {
    mode: 'update',
    initialTitle: '원래 제목',
    initialContent: '원래 본문 내용입니다.\n\n수정해보세요.',
  },
};

export const UpdateForbidden: Story = {
  args: {
    mode: 'update',
    initialTitle: '남의 글',
    initialContent: '본문',
    error: '본인의 글만 수정/삭제할 수 있습니다',
  },
};

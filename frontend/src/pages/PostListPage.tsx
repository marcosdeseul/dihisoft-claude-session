import { useCallback, useEffect, useState } from 'react';
import { listPosts, PostError, type PostPageResponse } from '../api/posts';
import { PostList } from '../components/posts/PostList';

const FALLBACK_ERROR = '게시글을 불러오지 못했습니다';

export default function PostListPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PostPageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback((p: number) => {
    setLoading(true);
    setError(null);
    listPosts(p, 10)
      .then((res) => {
        setData(res);
        setPage(res.page);
      })
      .catch((e) => {
        if (e instanceof PostError) setError(e.message || FALLBACK_ERROR);
        else setError(FALLBACK_ERROR);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(0);
  }, [load]);

  const onPrev = () => load(Math.max(0, page - 1));
  const onNext = () => load(page + 1);
  const onRetry = () => load(page);

  return (
    <PostList
      posts={data?.content ?? []}
      loading={loading}
      error={error}
      page={page}
      totalPages={data?.totalPages ?? 0}
      onPrev={onPrev}
      onNext={onNext}
      onRetry={onRetry}
    />
  );
}

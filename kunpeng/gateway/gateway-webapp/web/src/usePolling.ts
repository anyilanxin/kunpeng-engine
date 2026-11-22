import { useCallback, useEffect, useRef, useState } from 'react';

export interface Polling<T> {
  data: T | undefined;
  error: string | undefined;
  loading: boolean;
  refresh: () => Promise<void>;
}

/** 轮询钩子：interval 毫秒拉取 loader；错误保留上一份数据并记录消息 */
export function usePolling<T>(loader: () => Promise<T>, interval = 5000): Polling<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(true);
  const timer = useRef<number>();
  const alive = useRef(true);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const tick = useCallback(async () => {
    try {
      const next = await loaderRef.current();
      if (!alive.current) {
        return;
      }
      setData(next);
      setError(undefined);
    } catch (e) {
      if (!alive.current) {
        return;
      }
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      if (alive.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    alive.current = true;
    tick();
    timer.current = window.setInterval(tick, interval);
    return () => {
      alive.current = false;
      window.clearInterval(timer.current);
    };
  }, [tick, interval]);

  return { data, error, loading, refresh: tick };
}

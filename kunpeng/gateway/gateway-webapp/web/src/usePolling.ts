/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

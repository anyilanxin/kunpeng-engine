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
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export type ThemeMode = 'light' | 'dark';

const THEME_STORAGE_KEY = 'webapp-theme';

interface ThemeModeContextValue {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  toggleMode: () => void;
}

const ThemeModeContext = createContext<ThemeModeContextValue>({
  mode: 'light',
  setMode: () => {},
  toggleMode: () => {},
});

/** 初始模式：手动选过用所选，否则跟随系统 prefers-color-scheme（index.html 内联脚本已同步写 data-theme，此处保持一致） */
function resolveInitialMode(): ThemeMode {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
  } catch {
    // localStorage 不可用时按系统偏好
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function ThemeModeProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(resolveInitialMode);

  useEffect(() => {
    document.documentElement.dataset.theme = mode;
  }, [mode]);

  const value = useMemo<ThemeModeContextValue>(() => ({
    mode,
    setMode: (next) => {
      setModeState(next);
      try {
        localStorage.setItem(THEME_STORAGE_KEY, next);
      } catch {
        // 存储不可用时仅本次会话生效
      }
    },
    toggleMode: () => {
      setModeState((current) => {
        const next = current === 'dark' ? 'light' : 'dark';
        try {
          localStorage.setItem(THEME_STORAGE_KEY, next);
        } catch {
          // 存储不可用时仅本次会话生效
        }
        return next;
      });
    },
  }), [mode]);

  return <ThemeModeContext.Provider value={value}>{children}</ThemeModeContext.Provider>;
}

export function useThemeMode(): ThemeModeContextValue {
  return useContext(ThemeModeContext);
}

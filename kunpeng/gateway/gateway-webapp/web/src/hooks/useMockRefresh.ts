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
import { useCallback, useContext } from 'react';
import { ClusterDataContext } from '../data/ClusterDataContext';

/** 保留参考页的刷新交互，但触发真实 API 快照刷新（非定时 mock）；提示文案由 ClusterDataContext 统一 i18n */
export function useMockRefresh() {
  const { refreshing, refresh: refreshSnapshots } = useContext(ClusterDataContext);
  const refresh = useCallback(() => {
    void refreshSnapshots();
  }, [refreshSnapshots]);
  return { refreshing, refresh };
}

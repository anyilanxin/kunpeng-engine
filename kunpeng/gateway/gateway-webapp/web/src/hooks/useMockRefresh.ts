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

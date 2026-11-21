import { useCallback, useContext } from 'react';
import { ClusterDataContext } from '../data/ClusterDataContext';

/** 保留参考页的刷新交互，但触发真实 API 快照刷新（非定时 mock） */
export function useMockRefresh(_successText = '集群数据已刷新') {
  const { refreshing, refresh: refreshSnapshots } = useContext(ClusterDataContext);
  const refresh = useCallback(() => {
    void refreshSnapshots();
  }, [refreshSnapshots]);
  return { refreshing, refresh };
}

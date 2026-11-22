import { createContext, useContext, useEffect } from 'react';
import type { ReactNode } from 'react';
import { App } from 'antd';
import { useTranslation } from 'react-i18next';
import { api } from '../api';
import { usePolling } from '../usePolling';
import type { Broker, JobWorkerSnapshot } from '../types/cluster';

interface ClusterData {
  brokers: Broker[];
  workers: JobWorkerSnapshot[];
  refreshing: boolean;
  refresh: () => Promise<void>;
}

export const ClusterDataContext = createContext<ClusterData>({
  brokers: [],
  workers: [],
  refreshing: false,
  refresh: async () => {},
});

/** 控制台共享快照：broker/Worker 两个参考模型端点轮询，页面保持参考实现的只读消费方式 */
export function ClusterDataProvider({ children }: { children: ReactNode }) {
  const brokerQuery = usePolling<Broker[]>(() => api<Broker[]>('/cluster/brokers'), 6000);
  const workerQuery = usePolling<JobWorkerSnapshot[]>(
    () => api<JobWorkerSnapshot[]>('/cluster/workers'),
    6000,
  );
  const { message } = App.useApp();
  const { t } = useTranslation();

  // SSE 集群事件（membership 增删/可达性变化）：收到即静默重取，节点上下线秒级可见；轮询兜底
  const brokerRefresh = brokerQuery.refresh;
  const workerRefresh = workerQuery.refresh;
  useEffect(() => {
    const source = new EventSource('/api/v1/webapp/cluster/events');
    source.addEventListener('cluster', () => {
      void brokerRefresh();
      void workerRefresh();
    });
    return () => source.close();
  }, [brokerRefresh, workerRefresh]);

  const refresh = async () => {
    try {
      await Promise.all([brokerQuery.refresh(), workerQuery.refresh()]);
      message.success(t('app.common.refreshed'));
    } catch (e) {
      message.error(e instanceof Error ? e.message : t('app.common.refreshFailed'));
    }
  };

  return (
    <ClusterDataContext.Provider
      value={{
        brokers: brokerQuery.data ?? [],
        workers: workerQuery.data ?? [],
        refreshing: (brokerQuery.loading && !brokerQuery.data) || (workerQuery.loading && !workerQuery.data),
        refresh,
      }}
    >
      {children}
    </ClusterDataContext.Provider>
  );
}

export function useBrokers(): Broker[] {
  return useContext(ClusterDataContext).brokers;
}

export function useJobWorkers(): JobWorkerSnapshot[] {
  return useContext(ClusterDataContext).workers;
}

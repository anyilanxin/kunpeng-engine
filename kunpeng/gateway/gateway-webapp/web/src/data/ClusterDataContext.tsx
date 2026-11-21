import { createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import { App } from 'antd';
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

  const refresh = async () => {
    try {
      await Promise.all([brokerQuery.refresh(), workerQuery.refresh()]);
      message.success('集群数据已刷新');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '刷新失败');
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

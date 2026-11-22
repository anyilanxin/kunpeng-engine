export interface Envelope<T> {
  code: number;
  success: boolean;
  message: string;
  data: T;
  timestamp: number;
}

export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message);
  }
}

/** webapp API 请求：统一信封解包；401 抛 ApiError(401) 由上层转登录页 */
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api/v1/webapp${path}`, {
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  const body = (await response.json()) as Envelope<T>;
  if (!body.success) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}

/** 当前用户（filingInfo 为后台配置的底栏备案信息，空则前端不显示） */
export interface MeInfo {
  username: string;
  roles: string[];
  filingInfo?: string | null;
}

export async function login(username: string, password: string) {
  return api<MeInfo>('/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export async function logout() {
  return api<void>('/logout', { method: 'POST' });
}

export async function me() {
  return api<MeInfo>('/me');
}

/** 业务集群重新负载：apply=false 仅生成调度计划预览 */
export function dispatchBalance(apply: boolean) {
  return api<import('./types').DispatchOpVo>('/dispatch/balance', {
    method: 'POST',
    body: JSON.stringify({ apply }),
  });
}

/** 修改业务集群副本因子：apply=false 仅生成调度计划预览 */
export function dispatchChangeReplication(expectReplicationFactor: number, apply: boolean) {
  return api<import('./types').DispatchOpVo>('/dispatch/replication', {
    method: 'POST',
    body: JSON.stringify({ expectReplicationFactor, apply }),
  });
}

/** 修改业务集群分区数量：apply=false 仅生成调度计划预览 */
export function dispatchChangePartitions(expectPartitionsCount: number, apply: boolean) {
  return api<import('./types').DispatchOpVo>('/dispatch/partitions', {
    method: 'POST',
    body: JSON.stringify({ expectPartitionsCount, apply }),
  });
}

/** 时间戳(ms)转可读时间；-1/0 显示 - */
export function time(ms: number): string {
  if (!ms || ms < 0) {
    return '-';
  }
  return new Date(ms).toLocaleString('zh-CN', { hour12: false });
}

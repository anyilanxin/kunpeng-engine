export type PartitionRole =
  | 'INACTIVE'
  | 'PASSIVE'
  | 'PROMOTABLE'
  | 'FOLLOWER'
  | 'CANDIDATE'
  | 'LEADER';
export type PartitionHealth = 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN';

export interface Partition {
  group: string;
  id: number;
  role: PartitionRole;
  health: PartitionHealth;
  term: number;
  sourceId: number;
  agentSourceIds: number[];
}

export interface Broker {
  memberId: string;
  address: string;
  nodeSourceId: number;
  partitions: Partition[];
}

export interface WorkerSession {
  sessionId: number;
  worker: string;
  capacity: number;
  tenantIds: string[];
  fetchVariables: string[];
}

export interface GatewayConnection {
  jobType: string;
  sessions: WorkerSession[];
}

export interface Gateway {
  memberId: string;
  address: string;
  nodeSourceId: number;
  connections: GatewayConnection[];
}

export interface SubscriptionSession {
  gateway: string;
  address: string;
  sessionId: number;
  worker: string;
}

export interface JobSubscription {
  jobType: string;
  sessions: SubscriptionSession[];
}

export interface JobWorkerSnapshot {
  gateway: Gateway;
  subscriptions: JobSubscription[];
}

export interface PartitionCell extends Partition {
  broker: Broker;
}

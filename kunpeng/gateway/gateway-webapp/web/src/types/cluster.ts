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

/** 节点实时状态：注册表权威清单 × membership 可达性（本地扩展字段，verbatim 偏离点见技能） */
export type BrokerState = 'ONLINE' | 'OFFLINE' | 'UNREGISTERED';

export interface Broker {
  memberId: string;
  address: string | null;
  nodeSourceId: number;
  state?: BrokerState;
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

export interface BrokerRef {
  group: string;
  partitionId: number;
  role: string;
  health: string;
}

export interface BrokerVo {
  memberId: string;
  partitions: BrokerRef[];
}

export interface PartitionMemberVo {
  memberId: string;
  role: string;
  health: string;
  term: number;
  sourceId: number;
  agentSourceIds: number[];
}

export interface PartitionVo {
  group: string;
  partitionId: number;
  leader: string | null;
  members: PartitionMemberVo[];
  sourceId: number;
  active: boolean;
  frozen: boolean;
  businessAddress: string | null;
}

export interface TopologyVo {
  brokers: BrokerVo[];
  partitions: PartitionVo[];
}

export interface GatewayVo {
  nodeId: string;
  version: string;
  grpcAddress: string;
  allowClientDiscovery: boolean;
}

export interface JobStreamVo {
  streamId: number;
  worker: string;
  capacity: number;
  timeoutMillis: number;
  tenantIds: string[];
}

export interface JobTypeVo {
  jobType: string;
  streams: JobStreamVo[];
}

export interface PartitionMemberMetaVo {
  memberId: string;
  priority: number;
}

export interface PartitionInfoMetaVo {
  partitionGroup: string;
  partitionId: number;
  targetPriority: number;
  primaryMemberId: string;
  partitionMembers: PartitionMemberMetaVo[];
}

export interface DispatchExecutionVo {
  dispatchPlanExecutionId: number;
  executionOrder: number;
  state: string;
  memberId: string;
  message: string;
  startTime: number;
  endTime: number;
  dueDate: number;
  remaining: number;
  planType: string;
}

export interface DispatchPlanVo {
  dispatchPlanId: number;
  dispatchPlanType: string;
  dispatchState: string;
  applyPlan: boolean;
  initialize: boolean;
  oldPartitionsCount: number;
  expectPartitionsCount: number;
  oldReplicationFactor: number;
  expectReplicationFactor: number;
  executionPlan: DispatchExecutionVo[];
  meta: PartitionInfoMetaVo[];
  oldMeta: PartitionInfoMetaVo[];
  transferSourceId: number[];
}

export interface ClusterMetaVo {
  version: number;
  replicationFactor: number;
  currentReplicationFactor: number;
  currentPartitionCount: number;
  createTime: number;
  updateTime: number;
  meta: PartitionInfoMetaVo[];
  lastMeta: PartitionInfoMetaVo[];
}

export interface AdminClusterMetaVo {
  version: number;
  replicationFactor: number;
  currentReplicationFactor: number;
  createTime: number;
  updateTime: number;
  meta: PartitionInfoMetaVo;
  lastMeta: PartitionInfoMetaVo;
}

export interface AdminDispatchPlanVo {
  dispatchPlanId: number;
  dispatchPlanType: string;
  dispatchState: string;
  applyPlan: boolean;
  initialize: boolean;
  oldReplicationFactor: number;
  expectReplicationFactor: number;
  executionPlan: DispatchExecutionVo[];
  meta: PartitionInfoMetaVo;
  oldMeta: PartitionInfoMetaVo;
}

export interface DispatchVo {
  business: { clusterMeta: ClusterMetaVo; dispatchPlan: DispatchPlanVo } | null;
  admin: { clusterMeta: AdminClusterMetaVo; dispatchPlan: AdminDispatchPlanVo } | null;
}

/** 调度操作结果：成功时携带生成的调度计划与最新集群元数据 */
export interface DispatchOpVo {
  operation: string;
  success: boolean;
  code: number;
  message: string;
  dispatchPlan: DispatchPlanVo | null;
  clusterMeta: ClusterMetaVo | null;
}

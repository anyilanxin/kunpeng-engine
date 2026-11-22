import type { Broker, JobWorkerSnapshot, PartitionCell } from '../types/cluster';

export const getPartitionCells = (brokers: Broker[], group?: string): PartitionCell[] =>
  brokers.flatMap((broker) =>
    broker.partitions
      .filter((partition) => !group || partition.group === group)
      .map((partition) => ({ ...partition, broker })),
  );

export const getPartitionGroups = (brokers: Broker[]) =>
  Array.from(new Set(brokers.flatMap((broker) => broker.partitions.map((partition) => partition.group))));

export const getWorkerSessions = (snapshots: JobWorkerSnapshot[]) =>
  snapshots.flatMap(({ gateway }) =>
    gateway.connections.flatMap(({ jobType, sessions }) =>
      sessions.map((session) => ({ ...session, jobType, gateway })),
    ),
  );

export const formatJobType = (jobType: string) =>
  jobType
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

export const shortNodeName = (memberId: string) => memberId.replace('broker@', '');

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

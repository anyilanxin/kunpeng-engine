/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.atomix.raft.snapshot;

/** 快照类型：随快照元数据持久化，并随安装分片一起传输。 */
public enum SnapshotType {
  /** 常规快照：对当前状态周期性或按需创建的一致性快照。 */
  REGULAR,
  /** 引导快照：用于新节点/新分区引导启动的快照。 */
  BOOTSTRAP,
  /** 合并快照：多个快照合并压缩后产生的快照。 */
  MERGE
}

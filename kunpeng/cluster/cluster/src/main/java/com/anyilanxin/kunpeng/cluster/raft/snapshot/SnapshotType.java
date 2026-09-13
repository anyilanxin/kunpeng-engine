/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

/**
 * 快照类型：每种类型对应快照根目录（raft 根目录/snapshots）下的一个独立子目录。
 *
 * <p>{@link #RAFT} 为常规 raft 镜像（拍摄 + install/传输接收共用）；{@link #BOOTSTRAP} 为引导镜像（源分区
 * leader 拍摄、新分区引导节点拉取）；{@link #MERGE} 为合并镜像（分区删除迁移：源分区拍摄并推送，目标分区 接收后经业务
 * merge 合并）。后两者不参与常规保留策略，生命周期由引导引用计数/合并流程与节点关闭清理管控。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public enum SnapshotType {
  /** 常规快照（本地定时/手动拍摄，leader 用于 install 复制）。 */
  RAFT("snapshot"),
  /** 引导快照（新分区启动时跨分区拉取）。 */
  BOOTSTRAP("bootstrap"),
  /** 合并快照（分区删除时源 leader 推送到目标分区）。 */
  MERGE("merge");

  private final String directoryName;

  SnapshotType(final String directoryName) {
    this.directoryName = directoryName;
  }

  /** 相对快照根目录（raft 根目录/snapshots）的目录名。 */
  public String directoryName() {
    return directoryName;
  }
}

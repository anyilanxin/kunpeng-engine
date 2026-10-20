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
package com.anyilanxin.kunpeng.repository.business;

import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.repository.business.modules.state.FrozenStateRepository;
import java.nio.file.Path;
import java.util.List;

/**
 * 业务面仓储顶层接口：读写能力的统一入口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BusinessRepository extends MutableBusinessRepository, ImmutableBusinessRepository {
  TransactionContext getContext();

  BusinessRepositoryAppliers getAppliers();

  List<ResourceDataSplit> getResourceDataSplits();

  /** 冻结标记域（数据手术窗口本地持久化：引擎启动读取恢复冻结态，broker 重启不破防） */
  FrozenStateRepository frozenStateRepository();

  /**
   * 数据手术·拆分载荷写出：把指定资源的全部本地数据（与 {@link #getResourceDataSplits()} 同一过滤）写入 {@code snapshotDirectory}
   * 处的全新 RocksDB——目录形态与合并镜像一致，接收端走既有 merge 链路即可摄入。
   *
   * <p>须在分区冻结（双侧静止）窗口内调用：访问器遍历的是运行库。
   *
   * @param resourceId 资源 id
   * @param snapshotDirectory 目标目录（由镜像模块创建与管理）
   */
  void writeResourceData(int resourceId, Path snapshotDirectory);

  /**
   * 数据手术·删除被移走数据：移除本分区中归属于指定资源的全部本地数据。 正确性要求（见 cluster-dispatch-design 技能 03 号文档
   * §5）：仅在拆分安装确认成功之后、分区仍在冻结窗口内调用；幂等，重删无残留。
   *
   * @param resourceId 资源 id
   */
  void removeResourceData(int resourceId);
}

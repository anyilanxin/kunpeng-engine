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
package com.anyilanxin.kunpeng.repository.business.modules.state.record;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * 分区冻结标记 Entity：数据手术冻结窗口的本地持久化（行存在即冻结，值携带 drain 标志）—— broker 重启后引擎凭此恢复冻结态， 手术不因重启破防（见
 * cluster-dispatch-design 技能 03 号文档 §3.2）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class PartitionFrozenState extends UnpackedObject implements StoreValue {
  private final LongProperty drainProp = new LongProperty(1, "drain");

  public PartitionFrozenState() {
    super(1);
    declareProperty(drainProp);
  }

  public void setDrain(final boolean drain) {
    drainProp.setValue(drain ? 1 : 0);
  }

  public boolean isDrain() {
    return drainProp.getValue() == 1;
  }
}

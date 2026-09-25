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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.record;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 并行分发索引 Entity：并行分发索引信息的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DistributeParallelIndexRecordEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[DistributeParallelIndexRecordEntity]: 1,2
  private final LongProperty distributeIdProp = new LongProperty(1, "DISTRIBUTE_ID", -1);
  private final ArrayProperty<IntegerValue> distributeIndexProp =
      new ArrayProperty<>(2, "DISTRIBUTE_INDEX", IntegerValue::new);

  public DistributeParallelIndexRecordEntity() {
    super(2);
    // formatting:off
    declareProperty(distributeIdProp)
      .declareProperty(distributeIndexProp);
    // formatting:on
  }

  public long getDistributeId() {
    return distributeIdProp.getValue();
  }

  public DistributeParallelIndexRecordEntity setDistributeId(final long distributeId) {
    distributeIdProp.setValue(distributeId);
    return this;
  }

  public List<Integer> getDistributeIndex() {
    return StreamSupport.stream(distributeIndexProp.spliterator(), false)
        .map(IntegerValue::getValue)
        .collect(Collectors.toList());
  }

  public DistributeParallelIndexRecordEntity setDistributeIndex(
      final List<Integer> distributeIndexList) {
    distributeIndexProp.reset();
    if (!distributeIndexList.isEmpty()) {
      distributeIndexList.sort(Integer::compareTo);
      for (final Integer distributeIndex : distributeIndexList) {
        distributeIndexProp.add().setValue(distributeIndex);
      }
    }
    return this;
  }

  public DistributeParallelIndexRecordEntity removeDistributeIndex(final Integer distributeIndex) {
    final List<Integer> distributeIndexList = new ArrayList<>(getDistributeIndex());
    distributeIndexList.remove(distributeIndex);
    setDistributeIndex(distributeIndexList);
    return this;
  }
}

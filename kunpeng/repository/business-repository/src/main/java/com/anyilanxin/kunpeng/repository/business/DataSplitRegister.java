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

import java.util.ArrayList;
import java.util.List;

/**
 * 数据分片注册器：汇总各域的 RocksDB column family 分片。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DataSplitRegister {
  private final List<ResourceDataSplit> dataSplits = new ArrayList<>();

  public void register(ResourceDataSplit dataSplit) {
    dataSplits.add(dataSplit);
  }

  public List<ResourceDataSplit> getDataSplits() {
    return dataSplits;
  }
}

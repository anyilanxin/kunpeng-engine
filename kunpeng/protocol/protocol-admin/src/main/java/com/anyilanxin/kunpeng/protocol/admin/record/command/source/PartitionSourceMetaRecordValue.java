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
package com.anyilanxin.kunpeng.protocol.admin.record.command.source;

import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import java.util.List;

/**
 * 业务调度计划执行明细记录契约，描述所属计划执行 ID、执行顺序、调度起止时间、执行状态、补充说明与目标成员。
 *
 * @author zxuanhong
 * @since
 */
public interface PartitionSourceMetaRecordValue extends RecordValue {
  int getVersion();

  long getCreateTime();

  long getUpdateTime();

  int getMaxPartitionSourceId();

  List<PartitionSourceRecordValue> getSources();
}

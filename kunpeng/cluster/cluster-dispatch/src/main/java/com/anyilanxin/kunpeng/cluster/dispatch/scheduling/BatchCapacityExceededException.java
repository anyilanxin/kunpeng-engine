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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

/** 命令批次容量超限：拒绝入批时由 {@link HeapCommandBatch#append} 返回。 */
public class BatchCapacityExceededException extends RuntimeException {

  public BatchCapacityExceededException(
      final int recordLength, final int recordCount, final int batchBytes) {
    super(
        "Rejecting append of %d bytes; command batch capacity exhausted [records: %d, bytes: %d]"
            .formatted(recordLength, recordCount, batchBytes));
  }
}

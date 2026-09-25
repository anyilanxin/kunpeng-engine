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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.job;

import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobRecordValue;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.Collection;
import org.agrona.DirectBuffer;

/**
 * 激活一条 {@link JobRecordValue} 所需的最小属性集。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobActivationProperties extends BufferWriter {

  /**
   * 消费者名称（主要用作排障定位与归属展示）
   *
   * @see JobRecordValue#getWorker()
   */
  DirectBuffer worker();

  /**
   * 消费者点名要的变量名集合；空集合表示要全部变量
   *
   * @see JobRecordValue#getVariables()
   */
  Collection<DirectBuffer> fetchVariables();

  /**
   * 激活租约时长：过期后 job 重新回到可激活状态
   *
   * @see JobRecordValue#getDeadline()
   */
  long timeout();

  /**
   * 本次激活面向的租户标识集合
   *
   * @return 允许从中激活 job 的租户列表
   */
  Collection<String> tenantIds();
}

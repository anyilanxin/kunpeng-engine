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
package com.anyilanxin.kunpeng.broker.client.core;

import static com.anyilanxin.kunpeng.protocol.common.Protocol.DEPLOYMENT_PARTITION;

import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;

/**
 * 客户端请求抽象（面无关）：承载请求 Record 与目标分区描述。
 *
 * <p>requestType/valueType/lifeCycle 均为协议编号，语义由所属面别解释（编号池不合并）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ClientRequest<VALUE extends RequestRecordValue> {
  int RANDOM_PARTITION = 0;
  int DISTRIBUTE_PARTITION = DEPLOYMENT_PARTITION;
  int NOT_SPECIFIED_PARTITION = -1;

  long key();

  void setKey(long key);

  int partitionId();

  void setPartitionId(int partitionId);

  int requestType();

  int valueType();

  int lifeCycle();

  VALUE getValue();
}

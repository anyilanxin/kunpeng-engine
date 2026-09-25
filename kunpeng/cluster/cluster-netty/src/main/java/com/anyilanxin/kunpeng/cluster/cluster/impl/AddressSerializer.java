/*
 * Copyright 2018-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.cluster.impl;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import org.apache.fory.config.Config;
import org.apache.fory.context.ReadContext;
import org.apache.fory.context.WriteContext;
import org.apache.fory.serializer.Serializer;

/** Address 序列化器：host 字符串 + port 整数顺序读写。 */
public class AddressSerializer extends Serializer<Address> {

  /** 创建 {@link Address} 序列化器实例。 */
  public AddressSerializer(final Config config) {
    super(config, Address.class);
  }

  @Override
  public void write(final WriteContext writeContext, final Address address) {
    writeContext.writeString(address.host());
    writeContext.writeInt32(address.port());
  }

  @Override
  public Address read(final ReadContext readContext) {
    final String host = readContext.readString();
    final int port = readContext.readInt32();
    return Address.from(host, port);
  }
}

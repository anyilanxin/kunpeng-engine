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
package com.anyilanxin.kunpeng.broker.client.business;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import org.agrona.DirectBuffer;

/**
 * 业务面 API 请求写入器：将 API 请求编码到二进制缓冲。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ApiRequestWriter extends BufferWriter {

  ApiRequestWriter key(final long key);

  ApiRequestWriter requestType(final RecordType requestType);

  ApiRequestWriter valueType(final ValueType valueType);

  ApiRequestWriter lifeCycle(final ValueLifeCycle lifeCycle);

  ApiRequestWriter data(final DirectBuffer data);
}

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

import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import org.agrona.DirectBuffer;

/**
 * 统一传输请求读取器：从二进制缓冲解码 API 请求（面无关，编号语义由调用方解释）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ApiRequestReader extends BufferReader {

  void reset();

  long key();

  int requestType();

  int valueType();

  int lifeCycle();

  long requestTime();

  int resourceId();

  boolean hasData();

  DirectBuffer data();
}

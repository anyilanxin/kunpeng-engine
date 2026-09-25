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

import com.anyilanxin.kunpeng.protocol.business.record.RecordType;

/**
 * 主题工具：构造 broker 间通信的主题字符串。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class TopicUtils {
  private static final String API_TOPIC_PREFIX = "-api-";

  public static String getTopicName(
      final RecordType requestType, final String requestTypeResourceId) {
    return requestType.value() + API_TOPIC_PREFIX + requestTypeResourceId;
  }

  public static String getTopicName(
      final RecordType requestType, final Integer requestTypeResourceId) {
    return requestType.value() + API_TOPIC_PREFIX + requestTypeResourceId;
  }
}

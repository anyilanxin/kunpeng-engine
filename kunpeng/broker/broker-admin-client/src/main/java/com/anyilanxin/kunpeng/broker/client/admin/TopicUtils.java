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
package com.anyilanxin.kunpeng.broker.client.admin;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_PARTITION_SOURCE;

/**
 * @author zxuanhong
 * @since
 */
public class TopicUtils {
  private static final String API_TOPIC_PREFIX = "admin-api-";

  public static String getTopicName() {
    return API_TOPIC_PREFIX + ADMIN_PARTITION_SOURCE;
  }
}

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
package com.anyilanxin.kunpeng.configuration.gateway;

import java.time.Duration;

/** gateway 相关配置的默认值常量。 */
public final class ConfigurationDefaults {

  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final int DEFAULT_PORT = 26500;

  public static final String DEFAULT_CONTACT_POINT_HOST = "127.0.0.1";
  public static final int DEFAULT_CONTACT_POINT_PORT = 26502;

  public static final String DEFAULT_MAX_MESSAGE_SIZE = "4M";
  public static final int DEFAULT_MAX_MESSAGE_COUNT = 16;
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);
  public static final boolean DEFAULT_LONG_POLLING_ENABLED = true;
  public static final long DEFAULT_LONG_POLLING_TIMEOUT = 10_000;
  public static final int DEFAULT_LONG_POLLING_EMPTY_RESPONSE_THRESHOLD = 3;
  public static final boolean DEFAULT_TLS_ENABLED = false;
  public static final long DEFAULT_PROBE_TIMEOUT = 10_000; // 10 seconds

  public static final String DEFAULT_CLUSTER_NAME = "zeebe-cluster";
  public static final String DEFAULT_CLUSTER_MEMBER_ID = "gateway";
  public static final String DEFAULT_CLUSTER_HOST = "0.0.0.0";
  public static final int DEFAULT_CLUSTER_PORT = 26502;

  public static final int DEFAULT_MANAGEMENT_THREADS = 1;

  public static final String DEFAULT_KEEP_ALIVE_INTERVAL = "30s";
}

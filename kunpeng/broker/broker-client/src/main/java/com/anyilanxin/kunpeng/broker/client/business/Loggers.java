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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务客户端日志器集合。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Loggers {
  public static final Logger STREAM_PROCESSING =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.clustering");
  public static final Logger SYSTEM_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.system");
  public static final Logger TRANSPORT_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.transport");
  public static final Logger PROCESS_REPOSITORY_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.process.repository");
  public static final Logger LOGSTREAMS_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.logstreams");

  public static final Logger BPMN_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.bpmn");
  public static final Logger RAFT = LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.raft");
  public static final Logger SNAPSHOT_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.logstreams.snapshot");
}

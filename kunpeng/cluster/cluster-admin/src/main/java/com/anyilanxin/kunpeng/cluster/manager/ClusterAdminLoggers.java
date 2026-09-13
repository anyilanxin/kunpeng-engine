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
package com.anyilanxin.kunpeng.cluster.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 集群管理模块统一的日志 Logger 常量定义。 */
public final class ClusterAdminLoggers {
  public static final Logger CLUSTER_ADMIN =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.admin");

  public static final Logger CLUSTER_BUSINESS =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.business");
}

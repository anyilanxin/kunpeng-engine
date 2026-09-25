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
package com.anyilanxin.kunpeng.broker.monitoring;

/**
 * 磁盘空间可用性监听器：磁盘不可用时停止向事件日志写入，恢复后继续。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface DiskSpaceUsageListener {

  void onDiskSpaceNotAvailable();

  void onDiskSpaceAvailable();
}

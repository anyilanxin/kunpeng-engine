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

package com.anyilanxin.kunpeng.bpm.parse.dmn.util;

/**
 * 工具模块的日志聚合类，定义项目代码常量并持有 EnsureUtilLogger 与 IoUtilLogger 的共享实例。
 *
 * @author Sebastian Menski
 */
public class UtilsLogger extends BaseLogger {

  /** 工具模块的项目代码 */
  public static final String PROJECT_CODE = "UTILS";

  /** EnsureUtil 使用的共享日志实例 */
  public static final EnsureUtilLogger ENSURE_UTIL_LOGGER =
      BaseLogger.createLogger(
          EnsureUtilLogger.class, PROJECT_CODE, "org.camunda.commons.utils.ensure", "02");

  /** IoUtil 使用的共享日志实例 */
  public static final IoUtilLogger IO_UTIL_LOGGER =
      BaseLogger.createLogger(
          IoUtilLogger.class, PROJECT_CODE, "org.operaton.commons.utils.io", "01");
}

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

package com.anyilanxin.kunpeng.bpm.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解析模块的日志聚合类，集中定义模块内使用的 slf4j 日志记录器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmParseLogger {
  /** dmn 解析日志 */
  public static final Logger PARSE_DMN_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.bpm.parse.dmn");

  /** bpmn 解析日志 */
  public static final Logger PARSE_BPMN_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.bpm.parse.bpmn");

  private BpmParseLogger() {}
}

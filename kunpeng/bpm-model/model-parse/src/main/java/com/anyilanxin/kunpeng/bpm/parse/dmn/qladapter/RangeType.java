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
package com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter;

/**
 * @author zxuanhong
 * @since
 */
import java.util.regex.Pattern;

/** 区间类型枚举，每种类型对应一个独立的正则。 */
public enum RangeType {
  /** [start..end] — 闭-闭 */
  CLOSED_CLOSED(Pattern.compile("^\\[(.+)\\.\\.(.+)\\]$"), "rangeClosed(%s,%s)"),
  /** (start..end] — 开-闭 */
  OPEN_CLOSED(Pattern.compile("^\\((.+)\\.\\.(.+)\\]$"), "rangeClosedOpen(%s,%s)"),
  /** [start..end) — 闭-开 */
  CLOSED_OPEN(Pattern.compile("^\\[(.+)\\.\\.(.+)\\)$"), "rangeOpenClosed(%s,%s)"),
  /** (start..end) — 开-开 */
  OPEN_OPEN(Pattern.compile("^\\((.+)\\.\\.(.+)\\)$"), "rangeOpen(%s,%s)");

  private final Pattern pattern;
  private final String formatInfo;

  RangeType(final Pattern pattern, final String formatInfo) {
    this.pattern = pattern;
    this.formatInfo = formatInfo;
  }

  /** 返回该区间类型对应的匹配正则。 */
  public Pattern pattern() {
    return pattern;
  }

  /**
   * 用区间两端端点值按该类型的模板生成 QlExpress 区间函数表达式。
   *
   * @param leftValue 区间左端点表达式
   * @param rightValue 区间右端点表达式
   * @return 形如 rangeClosed(左值,右值) 的区间函数表达式
   */
  public String format(final String leftValue, final String rightValue) {
    return String.format(formatInfo, leftValue, rightValue);
  }
}

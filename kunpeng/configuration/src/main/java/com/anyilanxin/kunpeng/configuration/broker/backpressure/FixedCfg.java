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
package com.anyilanxin.kunpeng.configuration.broker.backpressure;

import static com.anyilanxin.kunpeng.configuration.broker.ConfigurationUtil.checkPositive;

public class FixedCfg {

  private int limit = 20;

  public int getLimit() {
    return limit;
  }

  public void setLimit(final int limit) {
    checkPositive(limit, "limit");
    this.limit = limit;
  }

  @Override
  public String toString() {
    return "FixedCfg{" + "limit=" + limit + '}';
  }
}

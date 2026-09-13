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
package com.anyilanxin.kunpeng.configuration.broker;

/** 一致性检查配置，控制前置条件校验与外键校验的开关。 */
public class ConsistencyCheckCfg {
  private static final boolean DEFAULT_ENABLE_PRECONDITIONS = false;
  private static final boolean DEFAULT_ENABLE_FOREIGN_KEY_CHECKS = false;
  private boolean enablePreconditions = DEFAULT_ENABLE_PRECONDITIONS;
  private boolean enableForeignKeyChecks = DEFAULT_ENABLE_FOREIGN_KEY_CHECKS;

  public boolean isEnablePreconditions() {
    return enablePreconditions;
  }

  public void setEnablePreconditions(final boolean enablePreconditions) {
    this.enablePreconditions = enablePreconditions;
  }

  public boolean isEnableForeignKeyChecks() {
    return enableForeignKeyChecks;
  }

  public void setEnableForeignKeyChecks(final boolean enableForeignKeyChecks) {
    this.enableForeignKeyChecks = enableForeignKeyChecks;
  }

  @Override
  public String toString() {
    return "ConsistencyCheckCfg{" + "enablePreconditions=" + enablePreconditions + '}';
  }
}

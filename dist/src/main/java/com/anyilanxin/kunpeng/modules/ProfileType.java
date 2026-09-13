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
package com.anyilanxin.kunpeng.modules;

public enum ProfileType {
  // application specific profiles
  BROKER("broker"),
  GATEWAY("gateway"),
  RESTORE("restore"),

  // environment profiles
  TEST("test"),
  DEVELOPMENT("dev"),
  PRODUCTION("prod"),
  STANDALONE("standalone");

  private final String id;

  ProfileType(final String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Profiles{ordinal='" + ordinal() + "', name='" + name() + "', id='" + id + '\'' + "}";
  }
}

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
package com.anyilanxin.kunpeng.engine.bpmn.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 授权实体解析：信封授权元数据 JSON → Authorization 的分支行为。 */
class AuthorizationTest {
  @Test
  void emptyMetadataYieldsNone() {
    assertSame(Authorization.NONE, Authorization.fromJson(null));
    assertSame(Authorization.NONE, Authorization.fromJson(""));
  }

  @Test
  void parsesUserIdAndGroups() {
    final Authorization authorization =
        Authorization.fromJson("{\"userId\":\"u1\",\"groups\":[\"g1\",\"g2\"]}");
    assertEquals("u1", authorization.userId());
    assertEquals(List.of("g1", "g2"), authorization.groups());
  }

  @Test
  void missingGroupsYieldsEmptyList() {
    final Authorization authorization = Authorization.fromJson("{\"userId\":\"u1\"}");
    assertEquals("u1", authorization.userId());
    assertTrue(authorization.groups().isEmpty());
  }

  @Test
  void missingUserIdFails() {
    assertThrows(
        IllegalArgumentException.class, () -> Authorization.fromJson("{\"groups\":[\"g1\"]}"));
  }

  @Test
  void nonListGroupsYieldsEmptyList() {
    final Authorization authorization =
        Authorization.fromJson("{\"userId\":\"u1\",\"groups\":\"g1\"}");
    assertTrue(authorization.groups().isEmpty());
  }
}

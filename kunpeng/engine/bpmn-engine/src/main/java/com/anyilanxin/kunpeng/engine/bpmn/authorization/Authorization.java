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

import static com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant.EMAIL;
import static com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant.FIRST_NAME;
import static com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant.GROUPS;
import static com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant.LAST_NAME;
import static com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant.USER_ID;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import java.util.List;
import java.util.Map;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 授权实体：信封授权元数据（JSON）解析出的具体身份，供引擎处理器直接使用。标准键见 {@link
 * UserMetadataConstant}（userId/groups/firstName/lastName/email/tenantId），校验只消费 userId 与
 * groups，其余为审计字段。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Authorization {
  /** 未携带授权元数据的操作人（未认证/系统语义） */
  public static final Authorization NONE = new Authorization("", "", "", "", "", List.of());

  private final String userId;
  private final String firstName;
  private final String lastName;
  private final String email;
  private final String tenantId;
  private final List<String> groups;

  private Authorization(
      final String userId,
      final String firstName,
      final String lastName,
      final String email,
      final String tenantId,
      final List<String> groups) {
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.tenantId = tenantId;
    this.groups = groups;
  }

  /** 从信封授权元数据 JSON 解析；空文本返回 {@link #NONE}，携带元数据但缺 userId 标准键直接失败。 */
  public static Authorization fromJson(final String json) {
    if (json == null || json.isEmpty()) {
      return NONE;
    }
    final Map<String, Object> metadata = convertToMap(new UnsafeBuffer(convertToMsgPack(json)));
    final Object userId = metadata.get(USER_ID);
    if (userId == null) {
      throw new IllegalArgumentException("授权元数据缺少标准键 userId");
    }
    final Object groups = metadata.get(GROUPS);
    final List<String> groupList;
    if (groups instanceof final List<?> list) {
      groupList = list.stream().map(String::valueOf).toList();
    } else {
      groupList = List.of();
    }
    return new Authorization(
        userId.toString(),
        text(metadata.get(FIRST_NAME)),
        text(metadata.get(LAST_NAME)),
        text(metadata.get(EMAIL)),
        text(metadata.get(TENANT_ID)),
        groupList);
  }

  private static String text(final Object value) {
    return value == null ? "" : value.toString();
  }

  /**
   * @return 操作人 id；未携带为空串
   */
  public String userId() {
    return userId;
  }

  /**
   * @return 操作人所属组 id；未携带为空列表
   */
  public List<String> groups() {
    return groups;
  }

  /**
   * @return 操作人名；未携带为空串
   */
  public String firstName() {
    return firstName;
  }

  /**
   * @return 操作人姓；未携带为空串
   */
  public String lastName() {
    return lastName;
  }

  /**
   * @return 操作人邮箱；未携带为空串
   */
  public String email() {
    return email;
  }

  /**
   * @return 操作人所属租户 id；未携带为空串
   */
  public String tenantId() {
    return tenantId;
  }
}

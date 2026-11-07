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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.AUTHORIZATION_ROLE_IDS;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.AUTHORIZATION_USER_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.AUTHORIZATION_USER_NAME;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import java.util.List;
import java.util.Map;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 授权实体：信封授权元数据（JSON）解析出的具体身份，供引擎处理器直接使用。标准键 userId/userName/roleIds，其余字段不参与校验。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Authorization {
  /** 未携带授权元数据的操作人（未认证/系统语义） */
  public static final Authorization NONE = new Authorization("", "", List.of());

  private final String userId;
  private final String userName;
  private final List<String> roleIds;

  private Authorization(final String userId, final String userName, final List<String> roleIds) {
    this.userId = userId;
    this.userName = userName;
    this.roleIds = roleIds;
  }

  /** 从信封授权元数据 JSON 解析；空文本返回 {@link #NONE}，携带元数据但缺 userId 标准键直接失败。 */
  public static Authorization fromJson(final String json) {
    if (json == null || json.isEmpty()) {
      return NONE;
    }
    final Map<String, Object> metadata = convertToMap(new UnsafeBuffer(convertToMsgPack(json)));
    final Object userId = metadata.get(AUTHORIZATION_USER_ID);
    if (userId == null) {
      throw new IllegalArgumentException("授权元数据缺少标准键 userId");
    }
    final Object userName = metadata.get(AUTHORIZATION_USER_NAME);
    final Object roleIds = metadata.get(AUTHORIZATION_ROLE_IDS);
    final List<String> roleIdList;
    if (roleIds instanceof final List<?> list) {
      roleIdList = list.stream().map(String::valueOf).toList();
    } else {
      roleIdList = List.of();
    }
    return new Authorization(
        userId.toString(), userName == null ? "" : userName.toString(), roleIdList);
  }

  /**
   * @return 操作人 id；未携带为空串
   */
  public String userId() {
    return userId;
  }

  /**
   * @return 操作人所属角色 id；未携带为空列表
   */
  public List<String> roleIds() {
    return roleIds;
  }

  /**
   * @return 操作人姓名；未携带为空串
   */
  public String userName() {
    return userName;
  }
}

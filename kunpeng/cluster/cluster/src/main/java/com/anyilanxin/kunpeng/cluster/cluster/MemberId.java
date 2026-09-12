/*
 * Copyright 2014-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.cluster;

import static com.anyilanxin.kunpeng.cluster.utils.MemberIdUtil.validateZone;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Controller cluster identity. */
@NullMarked
public class MemberId extends NodeId {

  /** Null when the member is not zone aware; the id is of the form {@code zone@suffix} */
  private final @Nullable String zone;

  public MemberId(final String id) {
    super(id);
    // '@' 是 zone 与节点标识的分隔符，validateZone 保证 zone 内不会出现 '@'
    final int sep = id.lastIndexOf('@');
    zone = sep > 0 ? validateZone(id.substring(0, sep)) : null;
  }

  /**
   * Creates a new anonymous cluster node identifier.
   *
   * @return node id
   */
  public static MemberId anonymous() {
    return new MemberId(UUID.randomUUID().toString());
  }

  /**
   * Creates a new cluster node identifier from the specified string.
   *
   * @param id string identifier
   * @return node id
   */
  public static MemberId from(final String id) {
    return new MemberId(id);
  }

  public @Nullable String zone() {
    return zone;
  }

  /**
   * @return if this memberId is bare, i.e. without a zone
   */
  public boolean isBare() {
    return zone == null;
  }

  /**
   * @return {@code true} if this member id belongs to the given zone.
   */
  public boolean isInZone(final @Nullable String zone) {
    return Objects.equals(this.zone, zone);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(final Object object) {
    return super.equals(object);
  }
}

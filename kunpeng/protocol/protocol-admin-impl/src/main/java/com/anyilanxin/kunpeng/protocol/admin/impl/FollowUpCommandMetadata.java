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
package com.anyilanxin.kunpeng.protocol.admin.impl;

import com.anyilanxin.kunpeng.protocol.admin.record.AdminRecordMetadataDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public record FollowUpCommandMetadata(long operationReference, Map<String, Object> claims) {

  public static FollowUpCommandMetadata empty() {
    return of(builder -> {});
  }

  public static FollowUpCommandMetadata of(final Consumer<Builder> consumer) {
    final Builder builder = new Builder();
    consumer.accept(builder);
    return builder.build();
  }

  public static class Builder {
    private long operationReference = AdminRecordMetadataDecoder.operationReferenceNullValue();
    private Map<String, Object> claims = null;

    public Builder operationReference(final long operationReference) {
      this.operationReference = operationReference;
      return this;
    }

    public Builder claims(final Map<String, Object> claims) {
      this.claims = claims;
      return this;
    }

    public Builder claim(final String key, final Object value) {
      if (claims == null) {
        claims = new HashMap<>();
      }

      claims.put(key, value);
      return this;
    }

    public FollowUpCommandMetadata build() {
      return new FollowUpCommandMetadata(operationReference, claims);
    }
  }
}

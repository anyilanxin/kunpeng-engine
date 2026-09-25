/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.business;

import com.anyilanxin.kunpeng.protocol.business.record.RecordMetadataDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public record FollowUpCommandMetadata(
    long operationReference, long batchOperationReference, Map<String, Object> claims) {

  public static FollowUpCommandMetadata empty() {
    return of(builder -> {});
  }

  public static FollowUpCommandMetadata of(final Consumer<Builder> consumer) {
    final Builder builder = new Builder();
    consumer.accept(builder);
    return builder.build();
  }

  public static class Builder {
    private long operationReference = RecordMetadataDecoder.operationReferenceNullValue();
    private long batchOperationReference = RecordMetadataDecoder.batchOperationReferenceNullValue();
    private Map<String, Object> claims = null;

    public Builder operationReference(final long operationReference) {
      this.operationReference = operationReference;
      return this;
    }

    public Builder batchOperationReference(final long batchOperationReference) {
      this.batchOperationReference = batchOperationReference;
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
      return new FollowUpCommandMetadata(operationReference, batchOperationReference, claims);
    }
  }
}

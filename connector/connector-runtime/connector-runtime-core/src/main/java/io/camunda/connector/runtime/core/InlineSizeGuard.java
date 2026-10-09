/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.camunda.connector.runtime.core;

import io.camunda.connector.api.error.ConnectorInputException;

public final class InlineSizeGuard {

  // Zeebe safe limit for commands that include variables (e.g. complete-job):
  // https://docs.camunda.io/docs/components/concepts/variables/#variable-size-limitation
  public static final long MAX_INLINE_BYTES = 3L * 1024 * 1024 / 2; // 1.5 MB

  private InlineSizeGuard() {}

  public static void check(long sizeBytes) {
    if (sizeBytes > MAX_INLINE_BYTES) {
      throw new ConnectorInputException(
          ("Output variables payload (%d bytes) exceeds the 1.5 MB safe variable size limit for"
                  + " Zeebe job completion. Reduce the size of the connector output variables.")
              .formatted(sizeBytes));
    }
  }
}

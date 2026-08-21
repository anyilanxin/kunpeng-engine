/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.atomix.raft.storage.system;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Persisted metadata of a Raft partition, serialized through the "Meta" SBE message defined in
 * raft-entry-schema.xml.
 */
public record MetaStoreRecord(long term, long lastFlushedIndex, long commitIndex, String votedFor) {

  public MetaStoreRecord {
    checkArgument(term >= 0, "term must be >= 0, but was: %d", term);
    checkArgument(
        lastFlushedIndex >= -1, "lastFlushedIndex must be >= -1, but was: %d", lastFlushedIndex);
    checkArgument(commitIndex >= -1, "commitIndex must be >= -1, but was: %d", commitIndex);
  }
}

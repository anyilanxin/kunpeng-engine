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
package com.anyilanxin.kunpeng.repository.business.modules.sink;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.sink.record.SinkStateEntry;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.agrona.DirectBuffer;
import org.agrona.collections.LongArrayList;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * sink 域仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SinkRepository implements MutableSinkRepository, RockResourceDataSplit {
  private final StringType sinkIdDbKey;
  private final ColumnFamily<StringType, SinkStateEntry> sinkColumnFamily;
  private static final UnsafeBuffer METADATA_NOT_FOUND = new UnsafeBuffer();

  public SinkRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    sinkIdDbKey = new StringType();
    sinkColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.SINK, transaction, sinkIdDbKey, new SinkStateEntry());
  }

  @Override
  public void setSinkState(final String sinkId, final long position, final DirectBuffer metadata) {
    sinkIdDbKey.wrapString(sinkId);
    final var sinkStateEntry = findSinkStateEntry(sinkId).orElse(new SinkStateEntry());
    sinkStateEntry.setPosition(position);
    if (metadata != null) {
      sinkStateEntry.setMetadata(metadata);
    }
    sinkColumnFamily.put(sinkIdDbKey, sinkStateEntry);
  }

  @Override
  public void initializeSinkState(
      final String sinkId,
      final long position,
      final DirectBuffer metadata,
      final long metadataVersion) {
    sinkIdDbKey.wrapString(sinkId);
    final var sinkStateEntry = new SinkStateEntry();
    sinkStateEntry.setPosition(position).setMetadataVersion(metadataVersion);
    if (metadata != null) {
      sinkStateEntry.setMetadata(metadata);
    }
    sinkColumnFamily.put(sinkIdDbKey, sinkStateEntry);
  }

  @Override
  public DirectBuffer getSinkMetadata(final String sinkId) {
    return findSinkStateEntry(sinkId).map(SinkStateEntry::getMetadata).orElse(METADATA_NOT_FOUND);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}

  @Override
  public long getMetadataVersion(final String sinkId) {
    return findSinkStateEntry(sinkId).map(SinkStateEntry::getMetadataVersion).orElse(0L);
  }

  private Optional<SinkStateEntry> findSinkStateEntry(final String sinkId) {
    sinkIdDbKey.wrapString(sinkId);
    return Optional.ofNullable(sinkColumnFamily.get(sinkIdDbKey));
  }

  @Override
  public void visitSinkState(final BiConsumer<String, SinkStateEntry> consumer) {
    sinkColumnFamily.forEach(
        (sinkId, sinkStateEntry) -> consumer.accept(sinkId.toString(), sinkStateEntry));
  }

  @Override
  public long getLowestPosition() {
    final LongArrayList positions = new LongArrayList();
    visitSinkState((sinkId, sinkStateEntry) -> positions.addLong(sinkStateEntry.getPosition()));
    return positions.longStream().min().orElse(-1L);
  }

  @Override
  public void removeSinkState(final String sinkId) {
    sinkIdDbKey.wrapString(sinkId);
    sinkColumnFamily.delete(sinkIdDbKey);
  }

  @Override
  public boolean hasSinks() {
    return !sinkColumnFamily.isEmpty();
  }

  @Override
  public void setSinkPosition(final String sinkId, final long position) {
    setSinkState(sinkId, position, null);
  }

  @Override
  public long getSinkPosition(final String sinkId) {
    return findSinkStateEntry(sinkId).map(SinkStateEntry::getPosition).orElse(VALUE_NOT_FOUND);
  }
}

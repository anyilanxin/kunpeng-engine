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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.utils.event.AbstractEvent;
import com.anyilanxin.kunpeng.cluster.utils.misc.TimestampPrinter;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/** Partition event. */
public class PartitionEvent extends AbstractEvent<PartitionEvent.Type, PartitionId> {

  private final Collection<MemberId> members;
  private final MemberId primary;
  private final Collection<MemberId> backups;

  public PartitionEvent(
      final Type type,
      final PartitionId partition,
      final Collection<MemberId> members,
      final MemberId primary,
      final Collection<MemberId> backups,
      final long time) {
    super(type, partition, time);
    this.members = checkNotNull(members);
    this.primary = primary;
    this.backups = checkNotNull(backups);
  }

  /**
   * Returns the partition ID.
   *
   * @return the partition ID
   */
  public PartitionId partitionId() {
    return subject();
  }

  /**
   * Returns the collection of partition members.
   *
   * @return the collection of partition members
   */
  public Collection<MemberId> members() {
    return members;
  }

  /**
   * Returns the current partition primary.
   *
   * @return the current partition primary
   */
  public Optional<MemberId> primary() {
    return Optional.ofNullable(primary);
  }

  /**
   * Returns the collection of backups.
   *
   * @return the collection of backups
   */
  public Collection<MemberId> backups() {
    return backups;
  }

  @Override
  public int hashCode() {
    return Objects.hash(partitionId(), members(), primary(), backups());
  }

  @Override
  public boolean equals(final Object object) {
    if (object instanceof PartitionEvent) {
      final PartitionEvent that = (PartitionEvent) object;
      return partitionId().equals(that.partitionId())
          && members.equals(that.members)
          && Objects.equals(primary, that.primary)
          && backups.equals(that.backups);
    }
    return false;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("time", new TimestampPrinter(time()))
        .add("type", type())
        .add("partitionId", subject())
        .add("members", members)
        .add("primary", primary)
        .add("backups", backups)
        .toString();
  }

  /** Partition event type. */
  public enum Type {
    /** Event type indicating the partition primary has changed. */
    PRIMARY_CHANGED,

    /** Event type indicating the partition backups have changed. */
    BACKUPS_CHANGED,

    /** Event type indicating the partition membership has changed. */
    MEMBERS_CHANGED,
  }
}

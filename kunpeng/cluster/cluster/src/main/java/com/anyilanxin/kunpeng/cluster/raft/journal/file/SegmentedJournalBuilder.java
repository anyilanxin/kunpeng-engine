/*
 * Copyright 2017-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalMetaStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import org.jspecify.annotations.Nullable;

/** Raft log builder. */
@SuppressWarnings("UnusedReturnValue")
public class SegmentedJournalBuilder {

  private static final String DEFAULT_NAME = "journal";
  private static final String DEFAULT_DIRECTORY = System.getProperty("user.dir");
  private static final int DEFAULT_MAX_SEGMENT_SIZE = 1024 * 1024 * 32;
  private static final long DEFAULT_MIN_FREE_DISK_SPACE = 1024L * 1024 * 1024;
  private static final int DEFAULT_JOURNAL_INDEX_DENSITY = 100;
  private static final boolean DEFAULT_VERIFY_READ_CHECKSUM = true;

  // impossible value to make it clear it's unset
  private static final int DEFAULT_PARTITION_ID = -1;

  protected String name = DEFAULT_NAME;
  protected File directory = new File(DEFAULT_DIRECTORY);
  protected int maxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;

  private long freeDiskSpace = DEFAULT_MIN_FREE_DISK_SPACE;
  private int journalIndexDensity = DEFAULT_JOURNAL_INDEX_DENSITY;
  private int partitionId = DEFAULT_PARTITION_ID;
  private boolean verifyReadChecksum = DEFAULT_VERIFY_READ_CHECKSUM;

  private @Nullable JournalMetaStore journalMetaStore;
  private final MeterRegistry meterRegistry;

  SegmentedJournalBuilder(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Sets the storage name.
   *
   * @param name The storage name.
   * @return The storage builder.
   */
  public SegmentedJournalBuilder withName(final String name) {
    this.name = checkNotNull(name, "name cannot be null");
    return this;
  }

  /**
   * Sets the journal directory, returning the builder for method chaining.
   *
   * <p>The journal will write segment files into the provided directory.
   *
   * @param directory The log directory.
   * @return The storage builder.
   * @throws NullPointerException If the {@code directory} is {@code null}
   */
  public SegmentedJournalBuilder withDirectory(final String directory) {
    return withDirectory(new File(checkNotNull(directory, "directory cannot be null")));
  }

  /**
   * Sets the journal directory, returning the builder for method chaining.
   *
   * <p>The journal will write segment files into the provided directory.
   *
   * @param directory The journal directory.
   * @return The journal builder.
   * @throws NullPointerException If the {@code directory} is {@code null}
   */
  public SegmentedJournalBuilder withDirectory(final File directory) {
    this.directory = checkNotNull(directory, "directory cannot be null");
    return this;
  }

  /**
   * Sets the maximum segment size in bytes, returning the builder for method chaining.
   *
   * <p>The maximum segment size dictates when logs should roll over to new segments. As entries are
   * written to a segment of the log, once the size of the segment surpasses the configured maximum
   * segment size, the log will create a new segment and append new entries to that segment.
   *
   * <p>By default, the maximum segment size is {@code 1024 * 1024 * 32}.
   *
   * @param maxSegmentSize The maximum segment size in bytes.
   * @return The journal builder.
   * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
   */
  public SegmentedJournalBuilder withMaxSegmentSize(final int maxSegmentSize) {
    checkArgument(
        maxSegmentSize > SegmentDescriptorSerializer.currentEncodingLength(),
        "maxSegmentSize must be greater than "
            + SegmentDescriptorSerializer.currentEncodingLength());
    this.maxSegmentSize = maxSegmentSize;
    return this;
  }

  /**
   * Sets the minimum free disk space to leave when allocating a new segment
   *
   * @param freeDiskSpace free disk space in bytes
   * @return the storage builder
   * @throws IllegalArgumentException if the {@code freeDiskSpace} is not positive
   */
  public SegmentedJournalBuilder withFreeDiskSpace(final long freeDiskSpace) {
    checkArgument(freeDiskSpace >= 0, "minFreeDiskSpace must be positive");
    this.freeDiskSpace = freeDiskSpace;
    return this;
  }

  public SegmentedJournalBuilder withJournalIndexDensity(final int journalIndexDensity) {
    this.journalIndexDensity = journalIndexDensity;
    return this;
  }

  /**
   * 设置读取路径是否校验记录 CRC32C。
   *
   * <p>默认开启。校验能发现静默位翻转，但热读路径上每条记录都要重算校验和；若上层已有 端到端校验（如 Raft 复制重算），可关闭以换取吞吐。恢复/重放扫描不受此开关影响， 始终校验。
   *
   * @param verifyReadChecksum 读取时是否校验 CRC
   * @return the journal builder for chaining
   */
  public SegmentedJournalBuilder withVerifyReadChecksum(final boolean verifyReadChecksum) {
    this.verifyReadChecksum = verifyReadChecksum;
    return this;
  }

  /**
   * The ID of the partition on which this journal resides. This is used primarily for
   * observability, e.g. in {@link JournalMetrics}.
   *
   * @param partitionId the journal's partition ID
   * @return this builder for chaining
   */
  public SegmentedJournalBuilder withPartitionId(final int partitionId) {
    this.partitionId = partitionId;
    return this;
  }

  /**
   * @param metaStore journal metastore to update lastFlushedIndex
   * @return this builder for chaining
   */
  public SegmentedJournalBuilder withMetaStore(final JournalMetaStore metaStore) {
    journalMetaStore = metaStore;
    return this;
  }

  public SegmentedJournal build() {
    final var journalIndex = new SparseJournalIndex(journalIndexDensity);
    final var journalMetrics = new JournalMetrics(name, meterRegistry);
    final var segmentLoader = new SegmentLoader(freeDiskSpace, journalMetrics);
    final var metaStore = requireNonNull(journalMetaStore, "must specify a journal meta store");
    final var segmentsManager =
        new SegmentsManager(
            journalIndex,
            maxSegmentSize,
            directory,
            name,
            segmentLoader,
            journalMetrics,
            metaStore,
            journalIndexDensity);

    return new SegmentedJournal(
        journalIndex, segmentsManager, journalMetrics, metaStore, verifyReadChecksum);
  }
}

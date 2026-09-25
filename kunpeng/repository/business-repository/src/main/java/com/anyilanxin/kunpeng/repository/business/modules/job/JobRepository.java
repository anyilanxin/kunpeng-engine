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
package com.anyilanxin.kunpeng.repository.business.modules.job;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KeyValuePairVisitor;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.RockResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.job.record.DeadlineIndex;
import com.anyilanxin.kunpeng.repository.business.modules.job.record.JobEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import org.agrona.DirectBuffer;

/**
 * job 域仓储实现：job 与 deadline 索引的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobRepository implements MutableJobRepository, RockResourceDataSplit {
  private final LongType jobIdDbKey;
  private final ColumnFamily<LongType, JobEntity> jobColumnFamily;
  private final JobEntity entityDbValue;
  private final JobRecord recordBuffer;
  // 激活的 job
  // 未激活的 job
  private final StringType jobTypeDbKey;
  private final StringType tenantIdDbValue;
  private final CompositeKeyType<StringType, LongType> jobTypeJobIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<StringType, LongType>, StringType>
      unActivatableColumnFamily;
  private final ColumnFamily<LongType, NilType> activatableColumnFamily;
  // 激活 job 的到期索引：(deadline, jobKey) 升序，超时检查器按前缀断点扫描
  private final LongType deadlineDbKey;
  private final CompositeKeyType<LongType, LongType> deadlineJobIdDbCompositeKey;
  private final ColumnFamily<CompositeKeyType<LongType, LongType>, NilType>
      deadlineIndexColumnFamily;

  public JobRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    splitRegister.register(this);
    recordBuffer = new JobRecord();
    jobIdDbKey = new LongType();
    entityDbValue = new JobEntity();
    jobColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.JOB, transaction, jobIdDbKey, entityDbValue);
    jobTypeDbKey = new StringType();
    tenantIdDbValue = new StringType();
    jobTypeJobIdDbCompositeKey = new CompositeKeyType<>(jobTypeDbKey, jobIdDbKey);
    unActivatableColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.JOB_UNACTIVITY,
            transaction,
            jobTypeJobIdDbCompositeKey,
            tenantIdDbValue);

    activatableColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.JOB_ACTIVITY,
            transaction,
            jobIdDbKey,
            NilType.INSTANCE);

    deadlineDbKey = new LongType();
    deadlineJobIdDbCompositeKey = new CompositeKeyType<>(deadlineDbKey, jobIdDbKey);
    deadlineIndexColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.JOB_DEADLINE,
            transaction,
            deadlineJobIdDbCompositeKey,
            NilType.INSTANCE);
  }

  @Override
  public void delete(final long key) {
    jobIdDbKey.wrapLong(key);
    entityDbValue.reset();
    if (jobColumnFamily.get(jobIdDbKey) != null) {
      removeDeadlineIndex(key, entityDbValue.getDueDate());
      tenantIdDbValue.wrapBuffer(entityDbValue.getTenantIdBuffer());
      jobTypeDbKey.wrapBuffer(entityDbValue.getJobTypeBuffer());
      jobColumnFamily.delete(jobIdDbKey);
      activatableColumnFamily.delete(jobIdDbKey);
      unActivatableColumnFamily.delete(jobTypeJobIdDbCompositeKey);
    }
  }

  @Override
  public void save(final long key, final JobRecord record) {
    jobIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    jobColumnFamily.put(jobIdDbKey, entityDbValue);
    unActivityJob(record);
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
    jobColumnFamily.forEach(
        (_, _) -> {
          if (check(jobIdDbKey.getValue(), resourceId)) {
            visitor.visit(
                writeKey(jobIdDbKey, BusinessRepositoryColumnFamilies.JOB),
                writeValue(entityDbValue));

            // save()/activated()/unActivated() 维护的激活状态索引(互斥, 按当前实际状态重建)
            if (activatableColumnFamily.exists(jobIdDbKey)) {
              visitor.visit(
                  writeKey(jobIdDbKey, BusinessRepositoryColumnFamilies.JOB_ACTIVITY),
                  writeValue(NilType.INSTANCE));
              final long dueDate = entityDbValue.getDueDate();
              if (dueDate > 0) {
                deadlineDbKey.wrapLong(dueDate);
                visitor.visit(
                    writeKey(
                        deadlineJobIdDbCompositeKey, BusinessRepositoryColumnFamilies.JOB_DEADLINE),
                    writeValue(NilType.INSTANCE));
              }
            } else {
              tenantIdDbValue.wrapBuffer(entityDbValue.getTenantIdBuffer());
              jobTypeDbKey.wrapBuffer(entityDbValue.getJobTypeBuffer());
              visitor.visit(
                  writeKey(
                      jobTypeJobIdDbCompositeKey, BusinessRepositoryColumnFamilies.JOB_UNACTIVITY),
                  writeValue(tenantIdDbValue));
            }
          }
        });
  }

  @Override
  public void update(final long key, final JobRecord record) {
    save(key, record);
  }

  private void unActivityJob(final JobRecord record) {
    tenantIdDbValue.wrapBuffer(record.getTenantIdBuffer());
    jobTypeDbKey.wrapBuffer(record.getJobTypeBuffer());
    unActivatableColumnFamily.put(jobTypeJobIdDbCompositeKey, tenantIdDbValue);
  }

  @Override
  public void activated(final long key, final JobRecord record) {
    jobIdDbKey.wrapLong(key);
    tenantIdDbValue.wrapBuffer(record.getTenantIdBuffer());
    jobTypeDbKey.wrapBuffer(record.getJobTypeBuffer());
    unActivatableColumnFamily.delete(jobTypeJobIdDbCompositeKey);

    activatableColumnFamily.put(jobIdDbKey, NilType.INSTANCE);
    final long dueDate = record.getDueDate();
    if (dueDate > 0) {
      deadlineDbKey.wrapLong(dueDate);
      deadlineIndexColumnFamily.put(deadlineJobIdDbCompositeKey, NilType.INSTANCE);
    }
  }

  @Override
  public void unActivated(final long key, final JobRecord record) {
    jobIdDbKey.wrapLong(key);
    activatableColumnFamily.delete(jobIdDbKey);
    removeDeadlineIndex(key, record.getDueDate());
    unActivityJob(record);
  }

  private void removeDeadlineIndex(final long key, final long dueDate) {
    if (dueDate > 0) {
      jobIdDbKey.wrapLong(key);
      deadlineDbKey.wrapLong(dueDate);
      deadlineIndexColumnFamily.delete(deadlineJobIdDbCompositeKey);
    }
  }

  @Override
  public Optional<JobRecord> query(final long key) {
    jobIdDbKey.wrapLong(key);
    return Optional.ofNullable(jobColumnFamily.get(jobIdDbKey)).map(v -> v.unwrap(recordBuffer));
  }

  @Override
  public JobRecord getRecord(final long key) {
    jobIdDbKey.wrapLong(key);
    if (jobColumnFamily.get(jobIdDbKey) != null) {
      return entityDbValue.unwrap(recordBuffer);
    }
    return null;
  }

  @Override
  public void processJobBatch(
      final DirectBuffer type,
      final List<String> tenantIds,
      final BiFunction<Long, JobRecord, Boolean> callback) {
    // HashSet conversion: whileEqualPrefix iterates per-job; contains on a List is O(n).
    // Single conversion here turns the inner check O(1), dominates List cost for >1 tenant.
    final Set<String> tenantSet = new HashSet<>(tenantIds);
    jobTypeDbKey.wrapBuffer(type);
    unActivatableColumnFamily.whileEqualPrefix(
        jobTypeDbKey,
        ((tenantAwareCompositeKey, tenantIdDbValue) -> {
          final LongType jobKey = tenantAwareCompositeKey.getSecond();
          final String tenantId = tenantIdDbValue.toString();
          if (tenantSet.contains(tenantId)) {
            return visitJob(jobKey.getValue(), callback::apply);
          }
          return true;
        }));
  }

  boolean visitJob(final long jobKey, final BiPredicate<Long, JobRecord> callback) {
    final Optional<JobRecord> query = query(jobKey);
    return query.map(record -> callback.test(jobKey, record)).orElse(true);
  }

  @Override
  public DeadlineIndex forEachTimedOutEntry(
      final long deadlineUpperBound,
      final DeadlineIndex startAt,
      final BiFunction<Long, JobRecord, Boolean> visitor) {
    if (startAt != null) {
      deadlineDbKey.wrapLong(startAt.deadline());
      jobIdDbKey.wrapLong(startAt.key());
    }
    final DeadlineIndex[] lastVisited = new DeadlineIndex[1];
    final boolean[] reachedBound = new boolean[1];
    if (startAt == null) {
      deadlineIndexColumnFamily.whileTrue(
          scanVisitor(deadlineUpperBound, visitor, lastVisited, reachedBound));
    } else {
      deadlineIndexColumnFamily.whileTrue(
          deadlineJobIdDbCompositeKey,
          scanVisitor(deadlineUpperBound, visitor, lastVisited, reachedBound));
    }
    // 触到上界即本轮扫完（断点作废）；仅在访问器主动截断时保留断点续扫。
    // 扫完但访问器未截断的尾部重访由 TIME_OUT 在途去重注册表兜底，无害。
    return reachedBound[0] ? null : lastVisited[0];
  }

  private KeyValuePairVisitor<CompositeKeyType<LongType, LongType>, NilType> scanVisitor(
      final long deadlineUpperBound,
      final BiFunction<Long, JobRecord, Boolean> visitor,
      final DeadlineIndex[] lastVisited,
      final boolean[] reachedBound) {
    return (key, ignored) -> {
      final long deadline = key.getFirst().getValue();
      if (deadline > deadlineUpperBound) {
        reachedBound[0] = true;
        return false;
      }
      final long jobKey = key.getSecond().getValue();
      lastVisited[0] = new DeadlineIndex(deadline, jobKey);
      return visitJob(jobKey, (k, record) -> visitor.apply(k, record));
    };
  }
}

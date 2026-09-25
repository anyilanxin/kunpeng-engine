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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.BpmnFactory;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.*;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ResourceDefinitionRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionEntity;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.ByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.MutableByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.DeploymentAndDefinitionRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record.DeploymentAndDefinitionType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.agrona.DirectBuffer;
import org.springframework.beans.factory.BeanFactory;

/**
 * BPMN 资源域仓储实现：流程定义与部署资源的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnResourceRepository implements MutableBpmnResourceRepository {
  private static final int LASTER_VERSION = -1;
  private final DeploymentAndDefinitionRepository deploymentAndDefinition;
  private final MutableByteArrayResourceRepository byteArrayResource;
  private final ProcessDefinitionRecord recordBuffer;
  // 公共部分
  private final StringType tenantIdDbKey;
  private final StringType processDefinitionKeyDbKey;
  // 流程定义基础信息
  private final ProcessDefinitionEntity entityDbValue;
  private final LongType processDefinitionIdDbKey;
  private final ColumnFamily<LongType, ProcessDefinitionEntity> processDefinitionColumnFamily;
  // 租户-流程定义-key:laster版本信息
  private final IntType processDefinitionVersionDbKey;
  private final CompositeKeyType<StringType, IntType> processDefinitionKeyVersionDbCompositeKey;
  private final TenantAwareKeyType<CompositeKeyType<StringType, IntType>>
      processDefinitionKeyVersionDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<CompositeKeyType<StringType, IntType>>, LongType>
      processDefinitionLasterVersionColumnFamily;

  private final TenantAwareKeyType<StringType> processDefinitionKeyLatestVersionDbTenantAwareKey;
  private final ColumnFamily<TenantAwareKeyType<StringType>, IntType>
      processDefinitionLasterLasterColumnFamily;

  private final Cache<String, ProcessDefinitionRuntime> cache;
  private final BpmnTransformer bpmnTransformer;

  public BpmnResourceRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister,
      final BeanFactory beanFactory,
      final MeterRegistry meterRegistry) {
    byteArrayResource = new ByteArrayResourceRepository(db, transaction, splitRegister);
    deploymentAndDefinition = new DeploymentAndDefinitionRepository(db, transaction, splitRegister);

    recordBuffer = new ProcessDefinitionRecord();
    // 公共部分
    tenantIdDbKey = new StringType();
    processDefinitionKeyDbKey = new StringType();
    // 流程定义基础信息
    entityDbValue = new ProcessDefinitionEntity();
    processDefinitionIdDbKey = new LongType();
    processDefinitionColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PROCESS_DEFINITION,
            transaction,
            processDefinitionIdDbKey,
            entityDbValue);
    processDefinitionVersionDbKey = new IntType();
    processDefinitionKeyVersionDbCompositeKey =
        new CompositeKeyType<>(processDefinitionKeyDbKey, processDefinitionVersionDbKey);
    // 租户-流程定义key
    processDefinitionKeyVersionDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey,
            processDefinitionKeyVersionDbCompositeKey,
            TenantAwareKeyType.PlacementType.PREFIX);
    processDefinitionLasterVersionColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PROCESS_DEFINITION_KEY_VERSION,
            transaction,
            processDefinitionKeyVersionDbTenantAwareKey,
            processDefinitionIdDbKey);

    processDefinitionKeyLatestVersionDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, processDefinitionKeyDbKey, TenantAwareKeyType.PlacementType.PREFIX);

    processDefinitionLasterLasterColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.PROCESS_DEFINITION_KEY_LASTER_VERSION,
            transaction,
            processDefinitionKeyLatestVersionDbTenantAwareKey,
            processDefinitionVersionDbKey);

    cache =
        Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .recordStats(() -> new CaffeineStatsCounter(meterRegistry, "process_definition_cache"))
            .build();
    bpmnTransformer = BpmnFactory.createTransformer(beanFactory);
  }

  @Override
  public void save(final long key, final ProcessDefinitionRecord record) {
    tenantIdDbKey.wrapString(record.getTenantId());
    processDefinitionKeyDbKey.wrapString(record.getProcessDefinitionKey());
    processDefinitionIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    processDefinitionColumnFamily.put(processDefinitionIdDbKey, entityDbValue);
    // 最新版本
    processDefinitionVersionDbKey.wrapInt(LASTER_VERSION);
    processDefinitionLasterVersionColumnFamily.put(
        processDefinitionKeyVersionDbTenantAwareKey, processDefinitionIdDbKey);

    // 最近版本信息
    processDefinitionVersionDbKey.wrapInt(record.getProcessDefinitionVersion());
    processDefinitionLasterVersionColumnFamily.put(
        processDefinitionKeyVersionDbTenantAwareKey, processDefinitionIdDbKey);

    // 最高版本信息
    processDefinitionLasterLasterColumnFamily.put(
        processDefinitionKeyLatestVersionDbTenantAwareKey, processDefinitionVersionDbKey);
    // 部署-定义关系
    deploymentAndDefinition.add(
        record.getDeploymentId(),
        record.getProcessDefinitionId(),
        DeploymentAndDefinitionType.PROCESS_DEFINITION);
    addCache(record);
  }

  @Override
  public int getVersion(final String processDefinitionKey, final String tenantId) {
    tenantIdDbKey.wrapString(tenantId);
    processDefinitionKeyDbKey.wrapString(processDefinitionKey);
    if (processDefinitionLasterLasterColumnFamily.get(
            processDefinitionKeyLatestVersionDbTenantAwareKey)
        != null) {
      return processDefinitionVersionDbKey.getValue() + 1;
    } else {
      return 1;
    }
  }

  @Override
  public void delete(final long key) {
    processDefinitionIdDbKey.wrapLong(key);
    if (processDefinitionColumnFamily.get(processDefinitionIdDbKey) != null) {
      removeCache(get(key));
      tenantIdDbKey.wrapString(recordBuffer.getTenantId());
      processDefinitionKeyDbKey.wrapString(recordBuffer.getProcessDefinitionKey());
      processDefinitionVersionDbKey.wrapInt(LASTER_VERSION);
      // 删除版本信息
      processDefinitionVersionDbKey.wrapInt(recordBuffer.getProcessDefinitionVersion());
      processDefinitionLasterVersionColumnFamily.delete(
          processDefinitionKeyVersionDbTenantAwareKey);

      // 删除定义信息
      processDefinitionColumnFamily.delete(processDefinitionIdDbKey);
      // 删除关系
      deploymentAndDefinition.remove(
          recordBuffer.getDeploymentId(), recordBuffer.getProcessDefinitionId());

      // 如果最新版本是当前删除的 key
      processDefinitionVersionDbKey.wrapInt(LASTER_VERSION);
      if (processDefinitionLasterVersionColumnFamily.get(
              processDefinitionKeyVersionDbTenantAwareKey)
          != null) {
        // 1. 删除最新版本标记
        processDefinitionLasterVersionColumnFamily.delete(
            processDefinitionKeyVersionDbTenantAwareKey);
        // 添加最新的最新版本信息
        final Long maxVersionProcessDefinitionId =
            getMaxVersionProcessDefinitionId(
                recordBuffer.getTenantId(), recordBuffer.getProcessDefinitionKey());
        if (maxVersionProcessDefinitionId != null) {
          processDefinitionIdDbKey.wrapLong(maxVersionProcessDefinitionId);
          processDefinitionLasterVersionColumnFamily.put(
              processDefinitionKeyVersionDbTenantAwareKey, processDefinitionIdDbKey);
        }
      }
    }
  }

  @Override
  public ProcessDefinitionRecord get(final long key) {
    processDefinitionIdDbKey.wrapLong(key);
    if (processDefinitionColumnFamily.get(processDefinitionIdDbKey) != null) {
      entityDbValue.unwrap(recordBuffer);
      final ResourceDefinitionRecord resourceDefinitionRecord =
          byteArrayResource.get(recordBuffer.getResourceDefinitionId());
      final DirectBuffer resourceBuffer = resourceDefinitionRecord.getResourceBuffer();
      recordBuffer.setResource(resourceBuffer, 0, resourceBuffer.capacity());
      return recordBuffer;
    }
    return null;
  }

  private Long getMaxVersionProcessDefinitionId(
      final String tenantId, final String processDefinitionKey) {
    final StringType dbTenantIdKey = new StringType();
    final StringType dbProcessDefinitionKey = new StringType();
    dbTenantIdKey.wrapString(tenantId);
    dbProcessDefinitionKey.wrapString(processDefinitionKey);
    final CompositeKeyType<StringType, StringType> tenantProcessDefinitionKey =
        new CompositeKeyType<>(dbTenantIdKey, dbProcessDefinitionKey);
    final AtomicLong lasterVersionProcessDefinitionId = new AtomicLong(-1);
    final AtomicInteger lasterVersion = new AtomicInteger(Integer.MAX_VALUE);
    processDefinitionLasterVersionColumnFamily.whileEqualPrefix(
        tenantProcessDefinitionKey,
        (dbCompositeKeyDbTenantAwareKey, dbLong) -> {
          final CompositeKeyType<StringType, IntType> dbStringDbIntDbCompositeKey =
              dbCompositeKeyDbTenantAwareKey.wrappedKey();
          final IntType second = dbStringDbIntDbCompositeKey.getSecond();
          if (second.getValue() < lasterVersion.get()) {
            lasterVersion.set(second.getValue());
            lasterVersionProcessDefinitionId.set(dbLong.getValue());
          }
        });
    if (lasterVersionProcessDefinitionId.get() == -1) {
      return null;
    } else {
      return lasterVersionProcessDefinitionId.get();
    }
  }

  @Override
  public void get(final long key, final DeploymentRecord record) {
    processDefinitionIdDbKey.wrapLong(key);
    if (processDefinitionColumnFamily.get(processDefinitionIdDbKey) != null) {
      final ProcessDefinitionRecord processDefinitionRecord = record.processDefinitions().add();
      entityDbValue.unwrap(processDefinitionRecord);
      addResourceInfo(processDefinitionRecord);
    }
  }

  @Override
  public ProcessDefinitionRecord get(
      final String processDefinitionKey, final String tenantId, final int version) {
    processDefinitionKeyDbKey.wrapString(processDefinitionKey);
    tenantIdDbKey.wrapString(tenantId);
    processDefinitionVersionDbKey.wrapInt(version);
    if (processDefinitionLasterVersionColumnFamily.get(processDefinitionKeyVersionDbTenantAwareKey)
        != null) {
      if (processDefinitionColumnFamily.get(processDefinitionIdDbKey) != null) {
        entityDbValue.unwrap(recordBuffer);
        addResourceInfo(recordBuffer);
        return recordBuffer;
      }
    }
    return null;
  }

  private void addResourceInfo(final ProcessDefinitionRecord definitionRecord) {
    final ResourceDefinitionRecord resourceDefinitionRecord =
        byteArrayResource.get(definitionRecord.getResourceDefinitionId());
    final DirectBuffer resourceBuffer = resourceDefinitionRecord.getResourceBuffer();
    final DirectBuffer checksumBuffer = resourceDefinitionRecord.getChecksumBuffer();
    definitionRecord.setResource(resourceBuffer, 0, resourceBuffer.capacity());
    definitionRecord.setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
  }

  @Override
  public ProcessDefinitionRecord get(final String processDefinitionKey, final String tenantId) {
    return get(processDefinitionKey, tenantId, -1);
  }

  @Override
  public void update(final long key, final ProcessDefinitionRecord record) {
    processDefinitionIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    processDefinitionColumnFamily.put(processDefinitionIdDbKey, entityDbValue);
    addCache(record);
  }

  @Override
  public ProcessDefinitionRuntime getRuntime(
      final String processDefinitionKey, final String tenantId, final int version) {
    final String cacheKey =
        getProcessDefinitionKeyCacheKey(processDefinitionKey, tenantId, version);
    final ProcessDefinitionRuntime ifPresent = cache.getIfPresent(cacheKey);
    if (ifPresent == null) {
      final ProcessDefinitionRecord processDefinitionRecord =
          get(processDefinitionKey, tenantId, version);
      if (processDefinitionRecord != null) {
        return addCache(processDefinitionRecord);
      }
    }
    return ifPresent;
  }

  @Override
  public ProcessDefinitionRuntime getRuntimeByDeployment(
      final long deploymentId, final String processDefinitionKey) {
    final List<Long> processDefinitionIds =
        deploymentAndDefinition.get(deploymentId, DeploymentAndDefinitionType.PROCESS_DEFINITION);
    if (processDefinitionIds.isEmpty()) {
      return null;
    } else {
      for (final Long processDefinitionId : processDefinitionIds) {
        final ProcessDefinitionRecord processDefinitionRecord = get(processDefinitionId);
        if (processDefinitionRecord != null
            && processDefinitionRecord.getProcessDefinitionKey().equals(processDefinitionKey)) {
          return getRuntime(processDefinitionRecord.getProcessDefinitionId());
        }
      }
    }
    return null;
  }

  private String getProcessDefinitionKeyCacheKey(
      final String processDefinitionKey, final String tenantId, final int version) {
    return processDefinitionKey + "-" + tenantId + "-" + version;
  }

  @Override
  public ProcessDefinitionRuntime getRuntime(
      final long processDefinitionId, final String tenantId) {
    final String cacheKey = getProcessDefinitionIdCacheKey(processDefinitionId, tenantId);
    final ProcessDefinitionRuntime ifPresent = cache.getIfPresent(cacheKey);
    if (ifPresent == null) {
      final ProcessDefinitionRecord processDefinitionRecord = get(processDefinitionId);
      if (processDefinitionRecord != null
          && processDefinitionRecord.getTenantId().equals(tenantId)) {
        return addCache(processDefinitionRecord);
      }
    }
    return ifPresent;
  }

  @Override
  public ProcessDefinitionRuntime getRuntime(final long processDefinitionId) {
    final ProcessDefinitionRuntime ifPresent =
        cache.getIfPresent(String.valueOf(processDefinitionId));
    if (ifPresent == null) {
      final ProcessDefinitionRecord processDefinitionRecord = get(processDefinitionId);
      if (processDefinitionRecord != null) {
        return addCache(processDefinitionRecord);
      }
    }
    return ifPresent;
  }

  private ProcessDefinitionRuntime addCache(final ProcessDefinitionRecord record) {
    final List<BpmnProcess> executableProcesses =
        bpmnTransformer.transformDefinitions(record.getResource());
    final Optional<BpmnProcess> first =
        executableProcesses.stream()
            .filter(v -> v.getId().equals(record.getProcessDefinitionKeyBuffer()))
            .findFirst();
    if (first.isPresent()) {
      final ProcessDefinitionRuntime runtime = new ProcessDefinitionRuntime(first.get(), record);
      cache.put(
          getProcessDefinitionIdCacheKey(record.getProcessDefinitionId(), record.getTenantId()),
          runtime);
      cache.put(String.valueOf(record.getProcessDefinitionId()), runtime);
      cache.put(
          getProcessDefinitionKeyCacheKey(
              record.getProcessDefinitionKey(),
              record.getTenantId(),
              record.getProcessDefinitionVersion()),
          runtime);

      tenantIdDbKey.wrapString(record.getTenantId());
      processDefinitionKeyDbKey.wrapString(record.getProcessDefinitionKey());
      processDefinitionVersionDbKey.wrapInt(LASTER_VERSION);
      final LongType dbLong =
          processDefinitionLasterVersionColumnFamily.get(
              processDefinitionKeyVersionDbTenantAwareKey);
      if (dbLong != null && dbLong.getValue() == record.getProcessDefinitionId()) {
        cache.put(
            getProcessDefinitionKeyCacheKey(
                record.getProcessDefinitionKey(), record.getTenantId(), -1),
            runtime);
      }
      return runtime;
    }
    return null;
  }

  private void removeCache(final ProcessDefinitionRecord record) {
    if (record != null) {
      cache.invalidate(
          getProcessDefinitionIdCacheKey(record.getProcessDefinitionId(), record.getTenantId()));
      cache.invalidate(
          getProcessDefinitionKeyCacheKey(
              record.getProcessDefinitionKey(),
              record.getTenantId(),
              record.getProcessDefinitionVersion()));
      cache.invalidate(String.valueOf(record.getProcessDefinitionId()));
      tenantIdDbKey.wrapString(record.getTenantId());
      processDefinitionKeyDbKey.wrapString(record.getProcessDefinitionKey());
      processDefinitionVersionDbKey.wrapInt(LASTER_VERSION);
      final LongType dbLong =
          processDefinitionLasterVersionColumnFamily.get(
              processDefinitionKeyVersionDbTenantAwareKey);
      if (dbLong != null && dbLong.getValue() == record.getProcessDefinitionId()) {
        cache.invalidate(
            getProcessDefinitionKeyCacheKey(
                record.getProcessDefinitionKey(), record.getTenantId(), -1));
      }
    }
  }

  private String getProcessDefinitionIdCacheKey(
      final long processDefinitionId, final String tenantId) {
    return processDefinitionId + "-" + tenantId;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}
}

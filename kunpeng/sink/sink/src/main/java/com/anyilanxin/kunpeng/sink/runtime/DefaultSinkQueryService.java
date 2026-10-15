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
package com.anyilanxin.kunpeng.sink.runtime;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionRecordValue;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.variable.ImmutableVariableRepository;
import com.anyilanxin.kunpeng.sink.query.SinkQueryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Sink 侧只读查询仓储实现：包装一个绑定分区的仓储聚合，直查 RocksDB。
 *
 * <p>线程安全：任意线程可调用。流程定义缓存命中（热路径）无锁；未命中装载与变量查询经 {@code synchronized(this)} 串行化——仓储读路径使用共享 key
 * 包装器与列族实体，必须独占，RocksDB 本身并发读写安全。并发同 key 未命中会各自回源一次（结果等价，后写覆盖），不额外去重。
 *
 * <p>缓存：流程定义一经部署内容不变，查询高频，按 id 缓存正本（容量/时效对齐 BpmnResourceRepository
 * 的既有缓存习惯）。命中返回缓存正本（只读视图，跨调用持有或需修改请自行 copy）； 装载时经 {@link ProcessDefinitionRecord#copy()}
 * 拷贝入缓存，条目互不共享。不缓存不存在 id 的负查询。
 *
 * <p>变量查询不经缓存（变量随流程执行变更，每次读取最新值）；{@code VariableRecord.getVariables()} 每次调用从 msgpack
 * 全新反序列化，返回映射天然为当次调用独享。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DefaultSinkQueryService implements SinkQueryService {
  private final ImmutableBpmnResourceRepository repository;
  private final ImmutableVariableRepository variableRepository;
  private final ImmutableActivityInstanceRepository activityInstanceRepository;
  private final Cache<Long, ProcessDefinitionRecord> processDefinitionCache;

  public DefaultSinkQueryService(final BusinessRepository repository) {
    Objects.requireNonNull(repository, "repository must not be null");
    this.repository = repository.bpmnResourceRepository();
    variableRepository = repository.variableRepository();
    activityInstanceRepository = repository.instanceRepository();
    processDefinitionCache =
        Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();
  }

  @Override
  public ProcessDefinitionRecordValue getProcessDefinition(final long processDefinitionId) {
    ProcessDefinitionRecord resultRecord = processDefinitionCache.getIfPresent(processDefinitionId);
    if (resultRecord == null) {
      synchronized (this) {
        // 仓储读路径使用共享 key 包装器、列族实体与复用 recordBuffer，必须锁内独占；
        // 返回的复用缓冲拷贝后才能入缓存，否则会被下次装载覆写
        final ProcessDefinitionRecord definitionRecord = repository.get(processDefinitionId);
        if (definitionRecord != null) {
          resultRecord = definitionRecord.copy();
          processDefinitionCache.put(processDefinitionId, resultRecord);
        }
      }
    }
    return resultRecord;
  }

  @Override
  public Map<String, Object> getVariable(final long processInstanceId) {
    synchronized (this) {
      return variableRepository
          .getRecord(processInstanceId)
          .map(VariableRecord::getVariables)
          .orElseGet(HashMap::new);
    }
  }

  @Override
  public Map<String, Object> getVariable(
      final long processInstanceId, final long activityInstanceId) {
    synchronized (this) {
      final Map<String, Object> variableMap = new HashMap<>();
      variableRepository
          .getRecord(activityInstanceId)
          .ifPresent(record -> variableMap.putAll(record.getVariables()));
      long currentActivityInstanceId = activityInstanceId;
      long currentParentActivityInstanceId;
      do {
        final ActivityInstanceRecord record =
            activityInstanceRepository.getRecord(currentActivityInstanceId);
        if (record != null) {
          currentParentActivityInstanceId = record.getParentActivityInstanceId();
          currentActivityInstanceId = currentParentActivityInstanceId;
          // 子作用域优先：父作用域变量只补齐子作用域未定义的键
          variableRepository
              .getRecord(currentParentActivityInstanceId)
              .ifPresent(
                  variableRecord ->
                      variableRecord.getVariables().forEach(variableMap::putIfAbsent));
        } else {
          variableRepository
              .getRecord(processInstanceId)
              .ifPresent(
                  variableRecord ->
                      variableRecord.getVariables().forEach(variableMap::putIfAbsent));
          break;
        }
      } while (processInstanceId != currentParentActivityInstanceId);
      return variableMap;
    }
  }
}

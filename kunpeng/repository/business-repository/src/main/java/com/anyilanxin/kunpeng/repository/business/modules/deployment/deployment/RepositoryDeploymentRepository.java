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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.kvstore.types.TenantAwareKeyType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.BpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.MutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.ByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.MutableByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record.DeploymentAndDefinition;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record.DeploymentEntity;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.DmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.MutableDmnResourceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;

/**
 * 部署域仓储实现：部署记录的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RepositoryDeploymentRepository implements MutableDeploymentRepository {
  private final StringType tenantIdDbKey;
  private final LongType deploymentIdDbKey;
  private final TenantAwareKeyType<LongType> deploymentIdDbTenantAwareKey;
  private final ColumnFamily<LongType, DeploymentEntity> deploymentColumnFamily;
  private final DeploymentEntity entityDbValue;
  private final DeploymentRecord recordBuffer;
  private final ColumnFamily<TenantAwareKeyType<LongType>, NilType> deploymentTemamtColumnFamily;

  private final DeploymentAndDefinitionRepository deploymentAndDefinition;
  private final MutableByteArrayResourceRepository byteArrayResource;
  private final MutableDmnResourceRepository dmnResource;
  private final MutableBpmnResourceRepository bpmnResource;

  public RepositoryDeploymentRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister,
      final BeanFactory beanFactory,
      final MeterRegistry meterRegistry) {
    byteArrayResource = new ByteArrayResourceRepository(db, transaction, splitRegister);
    dmnResource = new DmnResourceRepository(db, transaction, splitRegister);
    bpmnResource =
        new BpmnResourceRepository(db, transaction, splitRegister, beanFactory, meterRegistry);
    deploymentAndDefinition = new DeploymentAndDefinitionRepository(db, transaction, splitRegister);

    tenantIdDbKey = new StringType();
    deploymentIdDbKey = new LongType();
    deploymentIdDbTenantAwareKey =
        new TenantAwareKeyType<>(
            tenantIdDbKey, deploymentIdDbKey, TenantAwareKeyType.PlacementType.PREFIX);
    entityDbValue = new DeploymentEntity();
    recordBuffer = new DeploymentRecord();
    deploymentColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DEPLOYMENT,
            transaction,
            deploymentIdDbKey,
            entityDbValue);

    deploymentTemamtColumnFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DEPLOYMENT,
            transaction,
            deploymentIdDbTenantAwareKey,
            NilType.INSTANCE);
  }

  @Override
  public void save(final long key, final DeploymentRecord record) {
    deploymentIdDbKey.wrapLong(key);
    tenantIdDbKey.wrapString(record.getTenantId());
    entityDbValue.wrap(record);
    deploymentColumnFamily.put(deploymentIdDbKey, entityDbValue);
  }

  @Override
  public void update(final long key, final DeploymentRecord record) {
    save(key, record);
  }

  @Override
  public void delete(final long key) {
    deploymentIdDbKey.wrapLong(key);
    deploymentColumnFamily.delete(deploymentIdDbKey);
  }

  @Override
  public DeploymentRecord get(final long key, final String tenantId) {
    deploymentIdDbKey.wrapLong(key);
    tenantIdDbKey.wrapString(tenantId);
    if (deploymentTemamtColumnFamily.get(deploymentIdDbTenantAwareKey) != null) {
      deploymentColumnFamily.get(deploymentIdDbKey);
      entityDbValue.unwrap(recordBuffer);
      final List<DeploymentAndDefinition> deploymentAndDefinitions =
          deploymentAndDefinition.get(key);
      for (final DeploymentAndDefinition definition : deploymentAndDefinitions) {
        switch (definition.type()) {
          case DECISION_DEFINITION ->
              dmnResource.getDecision(definition.relationId(), recordBuffer);
          case DECISION_REQUIREMENT_DEFINITION ->
              dmnResource.getDecisionRequirement(definition.relationId(), recordBuffer);
          case PROCESS_DEFINITION -> bpmnResource.get(definition.relationId(), recordBuffer);
          case RESOURCE_DEFINITION -> byteArrayResource.get(definition.relationId(), recordBuffer);
        }
      }
      return recordBuffer;
    }
    return null;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}
}

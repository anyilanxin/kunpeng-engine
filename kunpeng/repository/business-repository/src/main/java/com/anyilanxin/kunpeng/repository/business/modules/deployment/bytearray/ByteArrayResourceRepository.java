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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray;

import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ResourceDefinitionRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.record.ResourceDefinitionEntity;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.DeploymentAndDefinitionRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record.DeploymentAndDefinitionType;

/**
 * 字节资源域仓储实现：部署资源内容的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ByteArrayResourceRepository implements MutableByteArrayResourceRepository {
  private final LongType resourceDefinitionIdDbKey;
  private final ResourceDefinitionRecord recordBuffer;
  private final ResourceDefinitionEntity entityDbValue;
  private final DeploymentAndDefinitionRepository deploymentAndDefinition;
  private final ColumnFamily<LongType, ResourceDefinitionEntity> deploymentAndDefinitionFamily;

  public ByteArrayResourceRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    deploymentAndDefinition = new DeploymentAndDefinitionRepository(db, transaction, splitRegister);
    entityDbValue = new ResourceDefinitionEntity();
    resourceDefinitionIdDbKey = new LongType();
    recordBuffer = new ResourceDefinitionRecord();
    deploymentAndDefinitionFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.RESOURCE_DEFINITION,
            transaction,
            resourceDefinitionIdDbKey,
            entityDbValue);
  }

  @Override
  public void save(final long key, final ResourceDefinitionRecord record) {
    resourceDefinitionIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    deploymentAndDefinitionFamily.put(resourceDefinitionIdDbKey, entityDbValue);

    deploymentAndDefinition.add(
        record.getDeploymentId(),
        record.getResourceDefinitionId(),
        DeploymentAndDefinitionType.RESOURCE_DEFINITION);
  }

  @Override
  public void update(final long key, final ResourceDefinitionRecord record) {
    resourceDefinitionIdDbKey.wrapLong(key);
    entityDbValue.wrap(record);
    deploymentAndDefinitionFamily.put(resourceDefinitionIdDbKey, entityDbValue);
  }

  @Override
  public void delete(final long key) {
    resourceDefinitionIdDbKey.wrapLong(key);
    deploymentAndDefinitionFamily.delete(resourceDefinitionIdDbKey);
    deploymentAndDefinition.remove(
        entityDbValue.getDeploymentId(), entityDbValue.getResourceDefinitionId());
  }

  @Override
  public ResourceDefinitionRecord get(final long key) {
    resourceDefinitionIdDbKey.wrapLong(key);
    if (deploymentAndDefinitionFamily.get(resourceDefinitionIdDbKey) != null) {
      entityDbValue.unwrap(recordBuffer);
      return recordBuffer;
    }
    return null;
  }

  @Override
  public void get(final long key, final DeploymentRecord record) {
    resourceDefinitionIdDbKey.wrapLong(key);
    if (deploymentAndDefinitionFamily.get(resourceDefinitionIdDbKey) != null) {
      entityDbValue.unwrap(record.resourceDefinitions().add());
    }
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}
}

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
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.EnumType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record.DeploymentAndDefinition;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record.DeploymentAndDefinitionType;
import java.util.ArrayList;
import java.util.List;

/**
 * 部署与定义关系仓储：部署和流程/决策定义的关联查询。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DeploymentAndDefinitionRepository {
  private final LongType deploymentIdDbKey;
  private final LongType relationIdDbKey;
  private final EnumType<DeploymentAndDefinitionType> relationTypeRepository;
  private final CompositeKeyType<LongType, LongType> deploymentIdRelationIdDbCompositeKey;
  private final ColumnFamily<
          CompositeKeyType<LongType, LongType>, EnumType<DeploymentAndDefinitionType>>
      deploymentAndDefinitionFamily;

  public DeploymentAndDefinitionRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    deploymentIdDbKey = new LongType();
    relationIdDbKey = new LongType();
    relationTypeRepository = new EnumType<>(DeploymentAndDefinitionType.class);
    deploymentIdRelationIdDbCompositeKey =
        new CompositeKeyType<>(deploymentIdDbKey, relationIdDbKey);

    deploymentAndDefinitionFamily =
        db.createColumnFamily(
            BusinessRepositoryColumnFamilies.DEPLOYMENT_AND_DEFINITION,
            transaction,
            deploymentIdRelationIdDbCompositeKey,
            relationTypeRepository);
  }

  public void add(
      final long deploymentId, final long relationId, final DeploymentAndDefinitionType type) {
    deploymentIdDbKey.wrapLong(deploymentId);
    relationIdDbKey.wrapLong(relationId);
    relationTypeRepository.setValue(type);

    deploymentAndDefinitionFamily.put(deploymentIdRelationIdDbCompositeKey, relationTypeRepository);
  }

  public void remove(final long deploymentId, final long relationId) {
    deploymentIdDbKey.wrapLong(deploymentId);
    relationIdDbKey.wrapLong(relationId);
    deploymentAndDefinitionFamily.delete(deploymentIdRelationIdDbCompositeKey);
  }

  public List<Long> get(final long deploymentId, final DeploymentAndDefinitionType type) {
    final List<Long> relationIds = new ArrayList<>();
    deploymentIdDbKey.wrapLong(deploymentId);
    deploymentAndDefinitionFamily.whileEqualPrefix(
        deploymentIdDbKey,
        (dbLongDbLongDbCompositeKey, deploymentAndDefinitionTypeEnumType) -> {
          if (type == deploymentAndDefinitionTypeEnumType.getValue()) {
            relationIds.add(dbLongDbLongDbCompositeKey.getSecond().getValue());
          }
        });
    return relationIds;
  }

  public List<DeploymentAndDefinition> get(final long deploymentId) {
    final List<DeploymentAndDefinition> relationInfos = new ArrayList<>();
    deploymentIdDbKey.wrapLong(deploymentId);
    deploymentAndDefinitionFamily.whileEqualPrefix(
        deploymentIdDbKey,
        (dbLongDbLongDbCompositeKey, deploymentAndDefinitionTypeEnumType) -> {
          final DeploymentAndDefinition deploymentAndDefinition =
              new DeploymentAndDefinition(
                  dbLongDbLongDbCompositeKey.getSecond().getValue(),
                  deploymentAndDefinitionTypeEnumType.getValue());
          relationInfos.add(deploymentAndDefinition);
        });
    return relationInfos;
  }
}

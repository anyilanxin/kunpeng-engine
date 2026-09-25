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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource;

import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionRequirementDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.business.DataSplitRegister;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.ByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.MutableByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.DeploymentAndDefinitionRepository;

/**
 * DMN 资源域仓储实现：决策定义与需求定义的读写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DmnResourceRepository implements MutableDmnResourceRepository {
  private final DeploymentAndDefinitionRepository deploymentAndDefinition;
  private final MutableByteArrayResourceRepository byteArrayResource;

  public DmnResourceRepository(
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final DataSplitRegister splitRegister) {
    byteArrayResource = new ByteArrayResourceRepository(db, transaction, splitRegister);
    deploymentAndDefinition = new DeploymentAndDefinitionRepository(db, transaction, splitRegister);
  }

  @Override
  public void saveDecision(final long key, final DecisionDefinitionRecord record) {}

  @Override
  public void updateDecision(final long key, final DecisionDefinitionRecord record) {}

  @Override
  public void deleteDecision(final long key) {}

  @Override
  public DecisionDefinitionRecord getDecision(final long key) {
    return null;
  }

  @Override
  public void getDecision(final long key, final DeploymentRecord record) {}

  @Override
  public int getDecisionVersion(final String decisionDefinitionKey, final String tenantId) {
    return 0;
  }

  @Override
  public void saveDecisionRequirement(
      final long key, final DecisionRequirementDefinitionRecord record) {}

  @Override
  public void updateDecisionRequirement(
      final long key, final DecisionRequirementDefinitionRecord record) {}

  @Override
  public void deleteDecisionRequirement(final long key) {}

  @Override
  public DecisionRequirementDefinitionRecord getDecisionRequirement(final long key) {
    return null;
  }

  @Override
  public void getDecisionRequirement(final long key, final DeploymentRecord record) {}

  @Override
  public int getDecisionRequirementVersion(
      final String decisionRequirementDefinitionKey, final String tenantId) {
    return 0;
  }

  @Override
  public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {}
}

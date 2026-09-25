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

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionRequirementDefinitionRecord;

/**
 * DMN 资源域可写仓储接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MutableDmnResourceRepository extends ImmutableDmnResourceRepository {
  void saveDecision(long key, DecisionDefinitionRecord record);

  void updateDecision(long key, DecisionDefinitionRecord record);

  void deleteDecision(long key);

  void saveDecisionRequirement(long key, DecisionRequirementDefinitionRecord record);

  void updateDecisionRequirement(long key, DecisionRequirementDefinitionRecord record);

  void deleteDecisionRequirement(long key);
}

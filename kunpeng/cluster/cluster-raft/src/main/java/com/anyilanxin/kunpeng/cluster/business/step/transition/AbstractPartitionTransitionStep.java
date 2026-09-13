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
package com.anyilanxin.kunpeng.cluster.business.step.transition;

import com.anyilanxin.kunpeng.cluster.business.PartitionStartupContext;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;

/**
 * @author zxuanhong
 * @since
 */
public abstract class AbstractPartitionTransitionStep<CONTENT extends PartitionStartupContext>
    implements StartupStep<CONTENT> {

  @Override
  public ActorFuture<CONTENT> shutdown(final CONTENT context) {
    final var result = context.getConcurrencyControl().<CONTENT>createFuture();
    final var partitionTransition = context.getPartitionTransition();
    if (partitionTransition == null) {
      result.complete(context);
      return result;
    }
    context
        .getConcurrencyControl()
        .runOnCompletion(
            partitionTransition.stop(),
            (ignored, failure) -> {
              if (failure == null) {
                context.getRaftPartition().removeRoleStateListener(partitionTransition);
                context.setPartitionTransition(null);
                result.complete(context);
              } else {
                result.completeExceptionally(failure);
              }
            });
    return result;
  }
}

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

import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;

/**
 * 分区角色切换上下文接口，承载切换过程中的并发控制及当前 term 与角色信息。
 *
 * @author zxuanhong
 * @since
 */
public interface TransitionContent<T> {
  ConcurrencyControl getConcurrencyControl();

  void setConcurrencyControl(ConcurrencyControl concurrencyControl);

  long getCurrentTerm();

  void setCurrentTerm(long currentTerm);

  RaftServer.Role getCurrentRole();

  void setCurrentRole(RaftServer.Role currentRole);
}

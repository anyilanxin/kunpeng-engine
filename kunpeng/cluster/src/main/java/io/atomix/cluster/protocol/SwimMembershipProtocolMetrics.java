/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.atomix.cluster.protocol;

import static io.atomix.cluster.protocol.SwimMembershipProtocolMetricsDoc.MEMBER_ADDED;
import static io.atomix.cluster.protocol.SwimMembershipProtocolMetricsDoc.MEMBER_COUNT;
import static io.atomix.cluster.protocol.SwimMembershipProtocolMetricsDoc.MEMBER_REMOVED;
import static io.atomix.cluster.protocol.SwimMembershipProtocolMetricsDoc.MEMBERS_INCARNATION_NUMBER;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.SettableGauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.ThreadSafe;

/** SWIM 成员协议相关指标采集 */
@ThreadSafe
final class SwimMembershipProtocolMetrics {

  private final MeterRegistry registry;
  private final Map<String, SettableGauge> incarnationNumbers = new ConcurrentHashMap<>();
  private final SettableGauge memberCount;

  SwimMembershipProtocolMetrics(final MeterRegistry registry) {
    this.registry = registry;
    memberCount = Micrometers.gauge(MEMBER_COUNT, registry);
  }

  /** 更新某成员的 incarnation 编号 */
  public void updateMemberIncarnationNumber(final String member, final long incarnationNumber) {
    incarnationNumbers
        .computeIfAbsent(
            member,
            id ->
                Micrometers.gauge(
                    MEMBERS_INCARNATION_NUMBER, registry, "memberId", id))
        .set(incarnationNumber);
  }

  /** 记录一次成员加入事件并刷新成员总数 */
  public void countMemberAdded(final int currentMemberCount) {
    Micrometers.counter(MEMBER_ADDED, registry).increment();
    memberCount.set(currentMemberCount);
  }

  /** 记录一次成员移除事件并刷新成员总数 */
  public void countMemberRemoved(final int currentMemberCount) {
    Micrometers.counter(MEMBER_REMOVED, registry).increment();
    memberCount.set(currentMemberCount);
  }
}

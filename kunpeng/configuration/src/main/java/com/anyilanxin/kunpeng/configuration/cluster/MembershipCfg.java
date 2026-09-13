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
package com.anyilanxin.kunpeng.configuration.cluster;

import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 成员协议（SWIM）配置，定义 gossip、探测与故障超时等参数。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
public final class MembershipCfg {
  // the following values are from atomix per default
  private static final boolean DEFAULT_BROADCAST_UPDATES = false;
  private static final boolean DEFAULT_BROADCAST_DISPUTES = true;
  private static final boolean DEFAULT_NOTIFY_SUSPECT = false;
  private static final Duration DEFAULT_GOSSIP_INTERVAL = Duration.ofMillis(250);
  private static final int DEFAULT_GOSSIP_FANOUT = 2;
  private static final Duration DEFAULT_PROBE_INTERVAL = Duration.ofMillis(1000);
  private static final Duration DEFAULT_PROBE_TIMEOUT = Duration.ofMillis(100);
  private static final int DEFAULT_SUSPECT_PROBES = 3;
  private static final Duration DEFAULT_FAILURE_TIMEOUT = Duration.ofMillis(10_000);
  private static final Duration DEFAULT_SYNC_INTERVAL = Duration.ofMillis(10_000);

  private boolean broadcastUpdates = DEFAULT_BROADCAST_UPDATES;
  private boolean broadcastDisputes = DEFAULT_BROADCAST_DISPUTES;
  private boolean notifySuspect = DEFAULT_NOTIFY_SUSPECT;
  private Duration gossipInterval = DEFAULT_GOSSIP_INTERVAL;
  private int gossipFanout = DEFAULT_GOSSIP_FANOUT;
  private Duration probeInterval = DEFAULT_PROBE_INTERVAL;
  private Duration probeTimeout = DEFAULT_PROBE_TIMEOUT;
  private int suspectProbes = DEFAULT_SUSPECT_PROBES;
  private Duration failureTimeout = DEFAULT_FAILURE_TIMEOUT;
  private Duration syncInterval = DEFAULT_SYNC_INTERVAL;
}

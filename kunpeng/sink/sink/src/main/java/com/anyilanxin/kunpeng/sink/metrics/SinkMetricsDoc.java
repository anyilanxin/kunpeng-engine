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
package com.anyilanxin.kunpeng.sink.metrics;

import com.anyilanxin.kunpeng.utils.micrometer.CustomMeterDocumentation;
import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.Meter.Type;

/**
 * Sink 运行时的指标目录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("NullableProblems")
public enum SinkMetricsDoc implements CustomMeterDocumentation {

  /** 按负载类型与动作统计交给 Sink 及被跳过的记录数。 */
  RECORDS {
    @Override
    public String getName() {
      return "kunpeng.sink.records";
    }

    @Override
    public Type getType() {
      return Type.COUNTER;
    }

    @Override
    public String getDescription() {
      return "Number of records the sink service processed, by action and value type";
    }

    @Override
    public KeyName[] getKeyNames() {
      return ActionKeyNames.values();
    }
  },

  /** 记录从写入日志到被 Sink 服务取起的等待时间。 */
  PICKUP_LATENCY {
    @Override
    public String getName() {
      return "kunpeng.sink.pickup.latency";
    }

    @Override
    public Type getType() {
      return Type.TIMER;
    }

    @Override
    public String getDescription() {
      return "Time between a record being written to the log and being picked up for processing";
    }
  },

  /** 单个 Sink 处理一条记录所花的时间。 */
  PROCESS_DURATION {
    @Override
    public String getName() {
      return "kunpeng.sink.process.duration";
    }

    @Override
    public Type getType() {
      return Type.TIMER;
    }

    @Override
    public String getDescription() {
      return "Time an sink needs to process a single record";
    }

    @Override
    public KeyName[] getKeyNames() {
      return new KeyName[] {TagKeyNames.SINK};
    }
  },

  /** 交给某个 Sink 的最后一条记录的位置。 */
  DELIVERED_POSITION {
    @Override
    public String getName() {
      return "kunpeng.sink.position.delivered";
    }

    @Override
    public Type getType() {
      return Type.GAUGE;
    }

    @Override
    public String getDescription() {
      return "Position of the last record delivered to an sink";
    }

    @Override
    public KeyName[] getKeyNames() {
      return new KeyName[] {TagKeyNames.SINK};
    }
  },

  /** 某个 Sink 状态已提交到的位置。 */
  COMMITTED_POSITION {
    @Override
    public String getName() {
      return "kunpeng.sink.position.committed";
    }

    @Override
    public Type getType() {
      return Type.GAUGE;
    }

    @Override
    public String getDescription() {
      return "Position up to which an sink acknowledged its records";
    }

    @Override
    public KeyName[] getKeyNames() {
      return new KeyName[] {TagKeyNames.SINK};
    }
  },

  /** Sink 服务生命周期阶段的数值表示。 */
  PHASE {
    @Override
    public String getName() {
      return "kunpeng.sink.phase";
    }

    @Override
    public Type getType() {
      return Type.GAUGE;
    }

    @Override
    public String getDescription() {
      return "Phase of the sink service: running (0), paused (1), soft paused (2), closed (3)";
    }
  };

  /** 上述指标共用的标签键。 */
  public enum TagKeyNames implements KeyName {
    /** 指标所属 Sink 的 id。 */
    SINK {
      @Override
      public String asString() {
        return "sink";
      }
    }
  }

  /** {@link #RECORDS} 指标 {@code action} 标签的取值。 */
  public enum ActionKeyNames implements KeyName {
    /** 记录已投递给至少一个 Sink 。 */
    DELIVERED {
      @Override
      public String asString() {
        return "delivered";
      }
    },

    /** 记录未命中任何 Sink ，未投递即被确认。 */
    SKIPPED {
      @Override
      public String asString() {
        return "skipped";
      }
    }
  }
}

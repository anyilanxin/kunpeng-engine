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
package com.anyilanxin.kunpeng.utils.micrometer;

import io.micrometer.core.instrument.docs.MeterDocumentation;
import java.time.Duration;

/** 指标文档接口：在 {@link MeterDocumentation} 基础上补充静态描述（help/description）与 SLO 桶定义，作为各模块指标枚举的统一基接口。 */
public interface CustomMeterDocumentation extends MeterDocumentation {

  /** 默认 Prometheus 直方图桶（严格递增、无重复）：亚毫秒级调度 → 毫秒级 IO → 秒级慢操作。 */
  Duration[] DEFAULT_PROMETHEUS_BUCKETS = {
    Duration.ofMillis(1),
    Duration.ofMillis(5),
    Duration.ofMillis(10),
    Duration.ofMillis(25),
    Duration.ofMillis(50),
    Duration.ofMillis(75),
    Duration.ofMillis(100),
    Duration.ofMillis(250),
    Duration.ofMillis(500),
    Duration.ofMillis(750),
    Duration.ofSeconds(1),
    Duration.ofMillis(2500),
    Duration.ofSeconds(5),
    Duration.ofMillis(7500),
    Duration.ofSeconds(10)
  };

  double[] EMPTY_DISTRIBUTION_SLOS = new double[0];

  /** 指标描述（Prometheus 的 help 文案） */
  String getDescription();

  /** TIMER 类指标的 SLO 桶 */
  default Duration[] getTimerSLOs() {
    return DEFAULT_PROMETHEUS_BUCKETS;
  }

  /** DISTRIBUTION_SUMMARY 类指标的 SLO 桶（默认空） */
  default double[] getDistributionSLOs() {
    return EMPTY_DISTRIBUTION_SLOS;
  }
}

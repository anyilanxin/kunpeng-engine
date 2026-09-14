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
package com.anyilanxin.kunpeng.bpm.parse.dmn.element;

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;

/**
 * 决策表命中策略（HitPolicy）类型枚举，将 DMN 模型中的 HitPolicy 与可选的内置聚合器
 * （BuiltinAggregator）组合映射为运行时使用的命中策略类型。
 *
 * @author zxuanhong
 * @since
 */
public enum HitPolicyType {
  /** 唯一命中（Unique） */
  UNIQUE(HitPolicy.UNIQUE, null),
  /** 首条命中（First） */
  FIRST(HitPolicy.FIRST, null),
  /** 优先级命中（Priority） */
  PRIORITY(HitPolicy.PRIORITY, null),
  /** 任一命中（Any） */
  ANY(HitPolicy.ANY, null),
  /** 规则顺序命中（Rule Order） */
  RULE_ORDER(HitPolicy.RULE_ORDER, null),
  /** 输出顺序命中（Output Order） */
  OUTPUT_ORDER(HitPolicy.OUTPUT_ORDER, null),
  /** 收集全部命中（Collect） */
  COLLECT(HitPolicy.COLLECT, null),
  /** 收集并计数（Collect Count） */
  COLLECT_COUNT(HitPolicy.COLLECT, BuiltinAggregator.COUNT),
  /** 收集并求和（Collect Sum） */
  COLLECT_SUM(HitPolicy.COLLECT, BuiltinAggregator.SUM),
  /** 收集并取最小（Collect Min） */
  COLLECT_MIN(HitPolicy.COLLECT, BuiltinAggregator.MIN),
  /** 收集并取最大（Collect Max） */
  COLLECT_MAX(HitPolicy.COLLECT, BuiltinAggregator.MAX),
  ;
  /** 对应的 DMN 模型命中策略 */
  private final HitPolicy hitPolicy;
  /** 内置聚合器，仅 Collect 类命中策略使用，其余为 null */
  private final BuiltinAggregator aggregator;

  HitPolicyType(final HitPolicy hitPolicy, final BuiltinAggregator aggregator) {
    this.hitPolicy = hitPolicy;
    this.aggregator = aggregator;
  }

  /**
   * 根据解析得到的 HitPolicy 与内置聚合器查找对应的命中策略类型。
   *
   * @param hitPolicy DMN 模型中的命中策略
   * @param aggregator 内置聚合器，非 Collect 类命中策略时为 null
   * @return 匹配的命中策略类型
   * @throws IllegalArgumentException 不存在与给定命中策略和聚合器匹配的枚举值时抛出
   */
  public static HitPolicyType getHitPolicyType(
      final HitPolicy hitPolicy, final BuiltinAggregator aggregator) {
    final HitPolicyType[] values = HitPolicyType.values();
    for (final HitPolicyType hitPolicyType : values) {
      if (hitPolicyType.hitPolicy == hitPolicy && hitPolicyType.aggregator == aggregator) {
        return hitPolicyType;
      }
    }
    throw new IllegalArgumentException("Invalid hitPolicy aggregator");
  }
}

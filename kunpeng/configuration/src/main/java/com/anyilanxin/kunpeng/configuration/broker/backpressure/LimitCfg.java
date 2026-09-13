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
package com.anyilanxin.kunpeng.configuration.broker.backpressure;

import com.anyilanxin.kunpeng.configuration.broker.ConfigurationEntry;
import com.anyilanxin.kunpeng.configuration.broker.backpressure.limit.StabilizingAIMDLimit;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.concurrency.limits.Limit;
import com.netflix.concurrency.limits.limit.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class LimitCfg implements ConfigurationEntry {

  private boolean enabled = true;

  @JsonProperty(value = "useWindowed")
  private boolean useWindowed = true;

  private LimitAlgorithm algorithm = LimitAlgorithm.AIMD;
  private final AIMDCfg aimd = new AIMDCfg();
  private final FixedCfg fixed = new FixedCfg();
  private final VegasCfg vegas = new VegasCfg();
  private final GradientCfg gradient = new GradientCfg();
  private final Gradient2Cfg gradient2 = new Gradient2Cfg();
  private final LegacyVegasCfg legacyVegas = new LegacyVegasCfg();

  public boolean isEnabled() {
    return enabled;
  }

  public LimitCfg setEnabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public boolean useWindowed() {
    return useWindowed;
  }

  public void setUseWindowed(final boolean useWindowed) {
    this.useWindowed = useWindowed;
  }

  public LimitAlgorithm getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(final String algorithm) {
    this.algorithm = LimitAlgorithm.valueOf(algorithm.toUpperCase());
  }

  public void setAlgorithm(final LimitAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public AIMDCfg getAimd() {
    return aimd;
  }

  public FixedCfg getFixed() {
    return fixed;
  }

  public VegasCfg getVegas() {
    return vegas;
  }

  public GradientCfg getGradient() {
    return gradient;
  }

  public Gradient2Cfg getGradient2() {
    return gradient2;
  }

  public LegacyVegasCfg getLegacyVegas() {
    return legacyVegas;
  }

  /**
   * @return null if disabled, (windowed) limit otherwise.
   */
  public Limit buildLimit() {
    if (!enabled) {
      return null;
    }
    final var baseLimit =
        switch (getAlgorithm()) {
          case AIMD -> getAIMD(getAimd());
          case FIXED -> FixedLimit.of(getFixed().getLimit());
          case GRADIENT -> getGradientLimit(getGradient());
          case GRADIENT2 -> getGradient2Limit(getGradient2());
          case VEGAS -> getVegasLimit(getVegas());
          case LEGACY_VEGAS -> getLegacyVegasLimit(getLegacyVegas());
        };
    if (useWindowed) {
      return WindowedLimit.newBuilder().build(baseLimit);
    } else {
      return baseLimit;
    }
  }

  private static VegasLimit getLegacyVegasLimit(final LegacyVegasCfg legacyVegas) {
    return VegasLimit.newBuilder()
        .alphaFunction(limit -> Math.max(3, (int) (limit * legacyVegas.alphaLimit())))
        .betaFunction(limit -> Math.max(6, (int) (limit * legacyVegas.betaLimit())))
        .initialLimit(legacyVegas.initialLimit())
        .maxConcurrency(legacyVegas.getMaxConcurrency())
        .increaseFunction(limit -> limit + Math.log(limit))
        .decreaseFunction(limit -> limit - Math.log(limit))
        .build();
  }

  private static VegasLimit getVegasLimit(final VegasCfg vegasCfg) {
    return VegasLimit.newBuilder()
        .alpha(vegasCfg.getAlpha())
        .beta(vegasCfg.getBeta())
        .initialLimit(vegasCfg.getInitialLimit())
        .build();
  }

  private static Gradient2Limit getGradient2Limit(final Gradient2Cfg gradient2Cfg) {
    return Gradient2Limit.newBuilder()
        .rttTolerance(gradient2Cfg.getRttTolerance())
        .initialLimit(gradient2Cfg.getInitialLimit())
        .minLimit(gradient2Cfg.getMinLimit())
        .longWindow(gradient2Cfg.getLongWindow())
        .build();
  }

  private static GradientLimit getGradientLimit(final GradientCfg gradientCfg) {
    return GradientLimit.newBuilder()
        .minLimit(gradientCfg.getMinLimit())
        .initialLimit(gradientCfg.getInitialLimit())
        .rttTolerance(gradientCfg.getRttTolerance())
        .build();
  }

  private static StabilizingAIMDLimit getAIMD(final AIMDCfg aimdCfg) {
    return StabilizingAIMDLimit.newBuilder()
        .initialLimit(aimdCfg.getInitialLimit())
        .minLimit(aimdCfg.getMinLimit())
        .maxLimit(aimdCfg.getMaxLimit())
        .expectedRTT(aimdCfg.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .backoffRatio(aimdCfg.getBackoffRatio())
        .build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        enabled, useWindowed, algorithm, aimd, fixed, vegas, gradient, gradient2, legacyVegas);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final LimitCfg limitCfg)) {
      return false;
    }
    return enabled == limitCfg.enabled
        && useWindowed == limitCfg.useWindowed
        && algorithm == limitCfg.algorithm
        && Objects.equals(aimd, limitCfg.aimd)
        && Objects.equals(fixed, limitCfg.fixed)
        && Objects.equals(vegas, limitCfg.vegas)
        && Objects.equals(gradient, limitCfg.gradient)
        && Objects.equals(gradient2, limitCfg.gradient2)
        && Objects.equals(legacyVegas, limitCfg.legacyVegas);
  }

  @Override
  public String toString() {
    return "LimitCfg{"
        + "enabled="
        + enabled
        + ", useWindowed="
        + useWindowed
        + ", algorithm='"
        + algorithm
        + '\''
        + ", aimd="
        + aimd
        + ", fixed="
        + fixed
        + ", vegas="
        + vegas
        + ", gradient="
        + gradient
        + ", gradient2="
        + gradient2
        + ", legacyVegas="
        + legacyVegas
        + '}';
  }

  public enum LimitAlgorithm {
    VEGAS,
    GRADIENT,
    GRADIENT2,
    FIXED,
    AIMD,
    LEGACY_VEGAS,
  }
}

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
package com.anyilanxin.kunpeng.client.spring.jobhandling.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link VariableResolver} 变量解析与类型映射测试
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class VariableResolverTest {

  private static final String JOB_TYPE = "order-process";

  private final JsonMapper jsonMapper = mock(JsonMapper.class);
  private final ActivatedJob job = mock(ActivatedJob.class);

  @BeforeEach
  void setUp() {
    when(job.getType()).thenReturn(JOB_TYPE);
  }

  @Test
  void shouldReturnValueWhenTypeMatches() {
    when(job.getVariablesAsMap()).thenReturn(Map.of("amount", 42));

    final var resolver = new VariableResolver("amount", Integer.class, jsonMapper, false);

    assertThat(resolver.resolve(null, job)).isEqualTo(42);
  }

  @Test
  void shouldConvertTypeViaJsonMapperWhenTypeDoesNotMatch() {
    when(job.getVariablesAsMap()).thenReturn(Map.of("amount", "42"));
    when(jsonMapper.toJson("42")).thenReturn("\"42\"");
    when(jsonMapper.fromJson("\"42\"", Integer.class)).thenReturn(42);

    final var resolver = new VariableResolver("amount", Integer.class, jsonMapper, false);

    assertThat(resolver.resolve(null, job)).isEqualTo(42);
  }

  @Test
  void shouldReturnNullWhenOptionalVariableMissing() {
    when(job.getVariablesAsMap()).thenReturn(Map.of());

    final var resolver = new VariableResolver("amount", Integer.class, jsonMapper, true);

    assertThat(resolver.resolve(null, job)).isNull();
  }

  @Test
  void shouldThrowWhenMandatoryVariableMissing() {
    when(job.getVariablesAsMap()).thenReturn(Map.of());

    final var resolver = new VariableResolver("amount", Integer.class, jsonMapper, false);

    assertThatThrownBy(() -> resolver.resolve(null, job))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("amount")
        .hasMessageContaining("mandatory");
  }

  @Test
  void shouldWrapConversionFailureWithJobContext() {
    when(job.getVariablesAsMap()).thenReturn(Map.of("amount", "not-a-number"));
    when(jsonMapper.toJson("not-a-number")).thenReturn("\"not-a-number\"");
    when(jsonMapper.fromJson(any(String.class), eq(Integer.class)))
        .thenThrow(new IllegalArgumentException("cannot parse"));

    final var resolver = new VariableResolver("amount", Integer.class, jsonMapper, false);

    assertThatThrownBy(() -> resolver.resolve(null, job))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("order-process")
        .hasMessageContaining("amount");
  }
}

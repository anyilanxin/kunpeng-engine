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

import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.spring.annotation.CustomHeaders;
import com.anyilanxin.kunpeng.client.spring.annotation.Variable;
import com.anyilanxin.kunpeng.client.spring.annotation.VariablesAsType;
import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import com.anyilanxin.kunpeng.client.spring.bean.ParameterInfo;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultParameterResolverStrategy} 按参数类型/注解选择解析器测试
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class DefaultParameterResolverStrategyTest {

  @SuppressWarnings("unused")
  static class Fixture {
    public void withJobClient(final JobClient client) {}

    public void withActivatedJob(final ActivatedJob job) {}

    public void withVariable(@Variable(name = "orderId") final String orderId) {}

    public void withVariablesAsType(@VariablesAsType final OrderPojo pojo) {}

    public void withCustomHeaders(@CustomHeaders final java.util.Map<String, String> headers) {}

    public void withPlainParameter(final String plain) {}
  }

  record OrderPojo(String orderId, int amount) {}

  private DefaultParameterResolverStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new DefaultParameterResolverStrategy(mockJsonMapper());
  }

  private static JsonMapper mockJsonMapper() {
    return org.mockito.Mockito.mock(JsonMapper.class);
  }

  private ParameterInfo parameterOf(final String methodName) throws Exception {
    final Method method = Fixture.class.getDeclaredMethod(methodName, parameterTypes(methodName));
    final var classInfo = ClassInfo.builder().bean(new Fixture()).beanName("fixture").build();
    return classInfo.toMethodInfo(method).getParameters().get(0);
  }

  private static Class<?>[] parameterTypes(final String methodName) throws Exception {
    for (final Method method : Fixture.class.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        return method.getParameterTypes();
      }
    }
    throw new NoSuchMethodException(methodName);
  }

  @Test
  void shouldResolveJobClientParameter() throws Exception {
    assertThat(strategy.createResolver(parameterOf("withJobClient")))
        .isInstanceOf(JobClientParameterResolver.class);
  }

  @Test
  void shouldResolveActivatedJobParameter() throws Exception {
    assertThat(strategy.createResolver(parameterOf("withActivatedJob")))
        .isInstanceOf(ActivatedJobParameterResolver.class);
  }

  @Test
  void shouldResolveAnnotatedVariableParameter() throws Exception {
    assertThat(strategy.createResolver(parameterOf("withVariable")))
        .isInstanceOf(VariableResolver.class);
  }

  @Test
  void shouldResolveVariablesAsTypeParameter() throws Exception {
    assertThat(strategy.createResolver(parameterOf("withVariablesAsType")))
        .isInstanceOf(VariablesAsTypeResolver.class);
  }

  @Test
  void shouldResolveCustomHeadersParameter() throws Exception {
    assertThat(strategy.createResolver(parameterOf("withCustomHeaders")))
        .isInstanceOf(CustomHeadersResolver.class);
  }

  @Test
  void shouldRejectPlainParameterWithoutAnnotation() throws Exception {
    assertThatThrownBy(() -> strategy.createResolver(parameterOf("withPlainParameter")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not create parameter resolver");
  }
}

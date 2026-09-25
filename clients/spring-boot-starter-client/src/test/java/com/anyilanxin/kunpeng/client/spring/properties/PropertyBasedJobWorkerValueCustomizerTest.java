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
package com.anyilanxin.kunpeng.client.spring.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil;
import com.anyilanxin.kunpeng.client.spring.annotation.JobWorker;
import com.anyilanxin.kunpeng.client.spring.annotation.Variable;
import com.anyilanxin.kunpeng.client.spring.annotation.VariablesAsType;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import com.anyilanxin.kunpeng.client.spring.bean.MethodInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link PropertyBasedJobWorkerValueCustomizer} 默认值填充、变量合并与覆写测试
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class PropertyBasedJobWorkerValueCustomizerTest {

  @SuppressWarnings("unused")
  static class Fixture {
    @JobWorker(type = "")
    public void plain() {}

    @JobWorker(type = "", fetchVariables = {"shared"})
    public void withVariables(
        @Variable(name = "orderId") final String orderId,
        @VariablesAsType final OrderPojo pojo) {}

    @JobWorker(type = "order-process")
    public void typed() {}

    @JobWorker(type = "", fetchAllVariables = true)
    public void fetchAll() {}
  }

  @SuppressWarnings("unused")
  static class OrderPojo {
    private String orderId;

    @JsonProperty("order_amount")
    private int amount;
  }

  private KunpengClientProperties properties;
  private PropertyBasedJobWorkerValueCustomizer customizer;

  @BeforeEach
  void setUp() {
    properties = new KunpengClientProperties();
    customizer = new PropertyBasedJobWorkerValueCustomizer(properties);
  }

  private JobWorkerValue jobWorkerValueOf(
      final String methodName, final Class<?>... parameterTypes) throws Exception {
    final Method method = Fixture.class.getDeclaredMethod(methodName, parameterTypes);
    final MethodInfo methodInfo =
        ClassInfo.builder().bean(new Fixture()).beanName("fixture").build().toMethodInfo(method);
    return AnnotationUtil.getJobWorkerValue(methodInfo).orElseThrow();
  }

  @Test
  void shouldGenerateNameAndTypeFromMethodInfo() throws Exception {
    final var value = jobWorkerValueOf("plain");

    customizer.customize(value);

    assertThat(value.getName()).isEqualTo("fixture#plain");
    assertThat(value.getType()).isEqualTo("plain");
    assertThat(value.getTimeout()).isEqualTo(Duration.ofMinutes(5));
    assertThat(value.getMaxJobsActive()).isEqualTo(32);
    assertThat(value.getPollInterval()).isEqualTo(Duration.ofMillis(100));
    assertThat(value.getFetchVariables()).isEmpty();
    assertThat(value.getTenantIds()).containsExactly("<default>");
  }

  @Test
  void shouldUseConfiguredDefaultNameAndTypeWhenPresent() throws Exception {
    properties.getWorker().getDefaults().setName("my-worker");
    properties.getWorker().getDefaults().setType("default-type");
    final var value = jobWorkerValueOf("plain");

    customizer.customize(value);

    assertThat(value.getName()).isEqualTo("my-worker");
    assertThat(value.getType()).isEqualTo("default-type");
  }

  @Test
  void shouldMergeFetchVariablesFromAnnotationVariablesAndPojoFields() throws Exception {
    final var value = jobWorkerValueOf("withVariables", String.class, OrderPojo.class);

    customizer.customize(value);

    assertThat(value.getFetchVariables())
        .containsExactly("shared", "orderId", "order_amount");
  }

  @Test
  void shouldForceFetchAllVariables() throws Exception {
    final var value = jobWorkerValueOf("fetchAll");

    customizer.customize(value);

    assertThat(value.getFetchVariables()).isEmpty();
  }

  @Test
  void shouldApplyWorkerOverrideByType() throws Exception {
    final var override = new KunpengClientJobWorkerProperties();
    override.setName("override-worker");
    override.setTimeout(Duration.ofSeconds(10));
    override.setFetchVariables(java.util.List.of("replaced"));
    properties.getWorker().getOverride().put("order-process", override);
    final var value = jobWorkerValueOf("typed");

    customizer.customize(value);

    assertThat(value.getName()).isEqualTo("override-worker");
    assertThat(value.getType()).isEqualTo("order-process");
    assertThat(value.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(value.getFetchVariables()).containsExactly("replaced");
  }

  @Test
  void shouldCombineTenantIdsFromClientAndAnnotation() throws Exception {
    properties.setTenantId("tenant-a");
    final var value = jobWorkerValueOf("plain");

    customizer.customize(value);

    assertThat(value.getTenantIds()).containsExactlyInAnyOrder("tenant-a", "<default>");
  }
}

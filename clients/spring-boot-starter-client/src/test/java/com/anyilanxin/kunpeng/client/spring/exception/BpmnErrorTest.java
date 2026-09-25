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
package com.anyilanxin.kunpeng.client.spring.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link BpmnError} 值承载与消息格式测试
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class BpmnErrorTest {

  @Test
  void shouldCarryErrorCodeMessageAndVariables() {
    final var variables = Map.of("amount", 42);
    final var error = new BpmnError("ORDER-7", "order rejected", variables, null);

    assertThat(error.getErrorCode()).isEqualTo("ORDER-7");
    assertThat(error.getErrorMessage()).isEqualTo("order rejected");
    assertThat(error.getVariables()).isSameAs(variables);
  }

  @Test
  void shouldFormatMessageAsBracketedErrorCode() {
    final var error = new BpmnError("ORDER-7", "order rejected");

    assertThat(error.getMessage()).isEqualTo("[ORDER-7] order rejected");
    assertThat(error.getVariables()).isNull();
  }
}

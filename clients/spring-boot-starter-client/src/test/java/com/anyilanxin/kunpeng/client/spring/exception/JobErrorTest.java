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

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link JobError} 值承载测试
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class JobErrorTest {

  @Test
  void shouldCarryRetriesBackoffAndVariables() {
    final var variables = Map.of("attempt", 3);
    final var cause = new IllegalStateException("boom");
    final var error =
        new JobError("job failed", variables, 5, Duration.ofSeconds(30), cause);

    assertThat(error.getErrorMessage()).isEqualTo("job failed");
    assertThat(error.getVariables()).isSameAs(variables);
    assertThat(error.getRetries()).isEqualTo(5);
    assertThat(error.getRetryBackoff()).isEqualTo(Duration.ofSeconds(30));
    assertThat(error.getCause()).isSameAs(cause);
  }

  @Test
  void shouldDefaultOptionalPayloadsToNull() {
    final var error = new JobError("job failed");

    assertThat(error.getErrorMessage()).isEqualTo("job failed");
    assertThat(error.getVariables()).isNull();
    assertThat(error.getRetries()).isNull();
    assertThat(error.getRetryBackoff()).isNull();
    assertThat(error.getMessage()).isEqualTo("job failed");
  }
}

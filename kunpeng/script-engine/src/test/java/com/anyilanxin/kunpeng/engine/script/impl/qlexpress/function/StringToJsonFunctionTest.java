/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 通过表达式引擎端到端测试 {@code toJson()} 函数。
 *
 * <p><b>注意：签名名为 {@code toJson}，但实现实际是 JSON 反序列化到 Map。</b>
 * 这与 FEEL 规范中 {@code toJson(value): string}（序列化）的语义相反。当前测试仅覆盖实现行为。
 */
class StringToJsonFunctionTest extends FunctionTestBase {

  @Test
  void parsesJsonObject() {
    // 实际实现是 JSON 反序列化：toJson("{\"a\": 1, \"b\": 2}") -> {a=1, b=2}
    assertThat(eval("toJson(\"{\\\"a\\\": 1, \\\"b\\\": 2}\")").toString()).isEqualTo("{a=1, b=2}");
  }

  @Test
  void parsesJsonWithSingleProperty() {
    assertThat(eval("toJson(\"{\\\"name\\\": \\\"test\\\"}\")").toString()).isEqualTo("{name=test}");
  }
}

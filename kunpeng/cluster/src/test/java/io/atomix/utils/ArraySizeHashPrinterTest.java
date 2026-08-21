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
package io.atomix.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.utils.misc.ArraySizeHashPrinter;
import org.junit.Test;

/** Array size hash printer test. */
public class ArraySizeHashPrinterTest {
  @Test
  public void testArraySizeHashPrinter() throws Exception {
    final ArraySizeHashPrinter printer = ArraySizeHashPrinter.of(new byte[] {1, 2, 3});
    assertThat(printer.toString()).isEqualTo("byte[]{length=3, hash=30817}");
  }
}

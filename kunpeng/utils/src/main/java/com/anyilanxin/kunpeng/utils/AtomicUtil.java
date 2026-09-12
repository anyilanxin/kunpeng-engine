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
package com.anyilanxin.kunpeng.utils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AtomicUtil {

  private AtomicUtil() {}

  public static <T> T replace(
      final AtomicReference<T> ref,
      final Function<T, Optional<T>> replacer,
      final Consumer<T> rollback) {
    T currentVal;
    T newVal;
    do {
      currentVal = ref.get();
      newVal = replacer.apply(currentVal).orElse(currentVal);
      if (ref.compareAndSet(currentVal, newVal)) {
        break;
      }
      rollback.accept(newVal);
    } while (true);
    return currentVal == newVal ? null : currentVal;
  }
}

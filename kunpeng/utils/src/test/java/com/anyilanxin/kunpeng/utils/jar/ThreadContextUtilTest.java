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
package com.anyilanxin.kunpeng.utils.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link ThreadContextUtil} 线程上下文类加载器切换测试 */
class ThreadContextUtilTest {

  private ClassLoader original;
  private ClassLoader replacement;

  @BeforeEach
  void captureOriginalContextLoader() {
    original = Thread.currentThread().getContextClassLoader();
    replacement = new ClassLoader(null) {};
  }

  @AfterEach
  void restoreOriginalContextLoader() {
    Thread.currentThread().setContextClassLoader(original);
  }

  @Test
  void runWithClassLoaderSwapsAndRestores() {
    final ClassLoader[] seen = new ClassLoader[1];

    ThreadContextUtil.runWithClassLoader(
        () -> seen[0] = Thread.currentThread().getContextClassLoader(), replacement);

    assertSame(replacement, seen[0]);
    assertSame(original, Thread.currentThread().getContextClassLoader());
  }

  @Test
  void runWithClassLoaderRestoresLoaderOnException() {
    final RuntimeException error = new RuntimeException("boom");

    final RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                ThreadContextUtil.runWithClassLoader(
                    () -> {
                      throw error;
                    },
                    replacement));

    assertSame(error, thrown);
    assertSame(original, Thread.currentThread().getContextClassLoader());
  }

  @Test
  void runCheckedWithClassLoaderSwapsAndRestores() throws Exception {
    final ClassLoader[] seen = new ClassLoader[1];

    ThreadContextUtil.runCheckedWithClassLoader(
        () -> seen[0] = Thread.currentThread().getContextClassLoader(), replacement);

    assertSame(replacement, seen[0]);
    assertSame(original, Thread.currentThread().getContextClassLoader());
  }

  @Test
  void runCheckedWithClassLoaderPropagatesCheckedExceptionAndRestores() {
    final IOException error = new IOException("boom");

    assertThrows(
        IOException.class,
        () ->
            ThreadContextUtil.runCheckedWithClassLoader(
                () -> {
                  throw error;
                },
                replacement));

    assertSame(original, Thread.currentThread().getContextClassLoader());
  }

  @Test
  void callWithClassLoaderSwapsAndReturnsResult() throws Exception {
    final String result =
        ThreadContextUtil.callWithClassLoader(
            () -> {
              assertSame(replacement, Thread.currentThread().getContextClassLoader());
              return "value";
            },
            replacement);

    assertEquals("value", result);
    assertSame(original, Thread.currentThread().getContextClassLoader());
  }

  @Test
  void getWithClassLoaderReturnsResult() {
    assertEquals("supplied", ThreadContextUtil.getWithClassLoader(() -> "supplied", replacement));
    assertSame(original, Thread.currentThread().getContextClassLoader());
  }
}

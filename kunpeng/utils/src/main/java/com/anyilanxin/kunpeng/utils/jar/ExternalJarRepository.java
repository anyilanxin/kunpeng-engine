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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 外部 jar 仓库：路径 → 加载器的单一登记表。同内容（SHA-256 相同）的 jar 无论路径几个，共享 同一个 {@link
 * ExternalJarClassLoader}；移除路径时仅当该加载器不再被任何路径引用才真正关闭。
 */
public final class ExternalJarRepository implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalJarRepository.class);
  private static final long MAX_JAR_BYTES = 100L * 1024 * 1024;

  /**
   * jar 扩展名
   */
  public static final String JAR_EXTENSION = ".jar";

  // 单一事实源: 路径 → (校验和, 加载器); 去重按校验和在此表上流式判定
  private final Map<Path, JarSlot> slots = new LinkedHashMap<>();

  public ExternalJarRepository() {
  }

  /**
   * 从既有登记表恢复（路径 → 加载器），按加载器自带校验和重建去重关系
   */
  public ExternalJarRepository(final Map<Path, ExternalJarClassLoader> loadedJars) {
    loadedJars.forEach(
      (path, loader) -> slots.put(path, new JarSlot(loader.getChecksum(), loader)));
  }

  /**
   * 路径 → 加载器只读视图
   */
  public Map<Path, ExternalJarClassLoader> getJars() {
    final Map<Path, ExternalJarClassLoader> view = new LinkedHashMap<>(slots.size());
    slots.forEach((path, slot) -> view.put(path, slot.loader()));
    return Collections.unmodifiableMap(view);
  }

  public ExternalJarClassLoader remove(final String jarPath) {
    return remove(Paths.get(jarPath));
  }

  /**
   * 移除路径登记；该加载器无其它路径引用时关闭并释放
   */
  public ExternalJarClassLoader remove(final Path jarPath) {
    final JarSlot slot = slots.remove(jarPath);
    if (slot == null) {
      return null;
    }
    final boolean stillReferenced =
      slots.values().stream().anyMatch(other -> other.loader() == slot.loader());
    if (!stillReferenced) {
      closeQuietly(jarPath, slot.loader());
    }
    return slot.loader();
  }

  public ExternalJarClassLoader load(final String jarPath) throws ExternalJarLoadException {
    return load(Paths.get(jarPath));
  }

  /**
   * 装载 jar：先做文件校验（扩展名/可读/非空/大小上限），再算校验和；同内容已装载则复用既有 加载器并把本路径挂到其名下。
   */
  public ExternalJarClassLoader load(final Path jarPath) throws ExternalJarLoadException {
    final JarSlot existing = slots.get(jarPath);
    if (existing != null) {
      return existing.loader();
    }
    verifyJarPath(jarPath);

    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarPath);
    final Optional<JarSlot> sameContent =
      slots.values().stream()
        .filter(slot -> slot.checksum().equals(loader.getChecksum()))
        .findFirst();
    if (sameContent.isPresent()) {
      closeQuietly(jarPath, loader);
      slots.put(jarPath, sameContent.orElseThrow());
      return sameContent.orElseThrow().loader();
    }
    slots.put(jarPath, new JarSlot(loader.getChecksum(), loader));
    return loader;
  }

  @Override
  public void close() throws Exception {
    slots.forEach((path, slot) -> closeQuietly(path, slot.loader()));
    slots.clear();
  }

  private void verifyJarPath(final Path path) throws ExternalJarLoadException {
    final String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
    if (!fileName.endsWith(JAR_EXTENSION)) {
      throw new ExternalJarLoadException(path, "当前文件不是 jar");
    }
    if (!Files.isReadable(path)) {
      throw new ExternalJarLoadException(path, "当前文件不可读");
    }
    final long size;
    try {
      size = Files.size(path);
    } catch (final IOException e) {
      throw new ExternalJarLoadException(path, "读取文件大小失败", e);
    }
    if (size == 0) {
      throw new ExternalJarLoadException(path, "文件为空");
    }
    if (size > MAX_JAR_BYTES) {
      throw new ExternalJarLoadException(path, "文件过大");
    }
  }

  private void closeQuietly(final Path path, final ExternalJarClassLoader loader) {
    try {
      if (!loader.isClosed()) {
        loader.close(false);
      }
    } catch (final IOException e) {
      LOGGER.warn("关闭外部 jar 加载器失败: {}", path, e);
    }
  }

  private record JarSlot(String checksum, ExternalJarClassLoader loader) {
  }
}

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 外部 jar 隔离加载器：直接以 {@link JarFile} 读取字节并 {@link ClassLoader#defineClass} 装载， jar
 * 内类优先（子优先），找不到再回退父加载器——外部导出器依赖版本与本进程互不干扰，同时 保证引擎自身类使用运行时版本。
 *
 * <p>持有 jar 内容的 SHA-256 校验和（同内容 jar 在仓库层共享同一加载器实例）。
 */
public final class ExternalJarClassLoader extends ClassLoader implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalJarClassLoader.class);
  private static final String CLASS_FILE_SUFFIX = ".class";
  private static final char PACKAGE_SEPARATOR = '.';

  static {
    registerAsParallelCapable();
  }

  private final Path jarPath;
  private final JarFile jarFile;
  private final String checksum;
  // 类名 → 已定义类缓存（defineClass 结果）
  private final Map<String, Class<?>> definedClasses = new HashMap<>();
  private volatile boolean closed;

  private ExternalJarClassLoader(final Path jarPath, final JarFile jarFile, final String checksum) {
    super(ExternalJarClassLoader.class.getClassLoader());
    this.jarPath = jarPath;
    this.jarFile = jarFile;
    this.checksum = checksum;
  }

  /** 从 jar 路径构建：校验和经 JDK {@link MessageDigest} 流式计算 */
  public static ExternalJarClassLoader ofPath(final Path jarPath) throws ExternalJarLoadException {
    final String checksum;
    try {
      checksum = sha256Hex(jarPath);
    } catch (final IOException e) {
      throw new ExternalJarLoadException(jarPath, "读取 jar 计算校验和失败", e);
    }
    try {
      return new ExternalJarClassLoader(jarPath, new JarFile(jarPath.toFile()), checksum);
    } catch (final IOException e) {
      throw new ExternalJarLoadException(jarPath, "打开 jar 文件失败", e);
    }
  }

  public String getChecksum() {
    return checksum;
  }

  public Path getJarPath() {
    return jarPath;
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() throws IOException {
    close(true);
  }

  /** 关闭加载器；verbose 时打印警告（提前关闭可能导致后续类加载 ClassNotFoundException） */
  public void close(final boolean verbose) throws IOException {
    if (verbose && !closed) {
      LOGGER.warn("关闭外部 jar 加载器 {}，后续类加载可能抛 ClassNotFoundException", jarPath);
    }
    if (!closed) {
      closed = true;
      definedClasses.clear();
      jarFile.close();
    }
  }

  /** 子优先装载：java.* 委派系统 → 已定义缓存 → jar 内定义 → 父加载器回退 */
  @Override
  protected Class<?> loadClass(final String name, final boolean resolve)
      throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      if (name.startsWith("java.")) {
        return getSystemClassLoader().loadClass(name);
      }
      Class<?> clazz = findLoadedClass(name);
      if (clazz == null) {
        clazz = definedClasses.get(name);
      }
      if (clazz == null) {
        clazz = defineFromJar(name);
      }
      if (clazz == null) {
        LOGGER.trace("jar {} 中无类 {}, 回退父加载器", jarPath, name);
        clazz = super.loadClass(name, false);
      }
      if (resolve) {
        resolveClass(clazz);
      }
      return clazz;
    }
  }

  private Class<?> defineFromJar(final String name) {
    final JarEntry entry = jarFile.getJarEntry(classResourceName(name));
    if (entry == null || entry.isDirectory()) {
      return null;
    }
    try (final InputStream stream = jarFile.getInputStream(entry)) {
      final byte[] bytes = stream.readAllBytes();
      final String packageName = packageNameOf(name);
      if (packageName != null && getDefinedPackage(packageName) == null) {
        definePackage(packageName, null, null, null, null, null, null, null);
      }
      final Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
      definedClasses.put(name, clazz);
      return clazz;
    } catch (final IOException e) {
      LOGGER.trace("读取 jar {} 类 {} 字节失败", jarPath, name, e);
      return null;
    }
  }

  private static String classResourceName(final String className) {
    return className.replace(PACKAGE_SEPARATOR, '/') + CLASS_FILE_SUFFIX;
  }

  private static String packageNameOf(final String className) {
    final int lastDot = className.lastIndexOf(PACKAGE_SEPARATOR);
    return lastDot < 0 ? null : className.substring(0, lastDot);
  }

  private static String sha256Hex(final Path file) throws IOException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM 缺少 SHA-256 算法", e);
    }
    final byte[] buffer = new byte[8192];
    try (final InputStream in = Files.newInputStream(file)) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }
    final StringBuilder hex = new StringBuilder(digest.getDigestLength() * 2);
    for (final byte b : digest.digest()) {
      hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return hex.toString();
  }
}

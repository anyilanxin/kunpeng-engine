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
package com.anyilanxin.kunpeng.bpm.parse.dmn.util;

import com.anyilanxin.kunpeng.bpm.parse.dmn.exception.IoUtilException;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * IO 工具类，提供输入流、Reader、文件与字符串之间的转换以及 classpath 文件加载等静态方法，字符编码统一使用
 * UTF-8。
 *
 * @author Sebastian Menski
 */
public class IoUtil {
  private static final IoUtilLogger LOG = UtilsLogger.IO_UTIL_LOGGER;
  public static final Charset ENCODING_CHARSET = StandardCharsets.UTF_8;

  protected IoUtil() {}

  /**
   * 将输入流以字符串形式返回。
   *
   * @param inputStream 输入流
   * @return 输入流对应的字符串。
   */
  public static String inputStreamAsString(final InputStream inputStream) {
    return new String(inputStreamAsByteArray(inputStream), ENCODING_CHARSET);
  }

  /**
   * 将输入流以 byte[] 形式返回。
   *
   * @param inputStream 输入流
   * @return 输入流对应的 byte[]。
   */
  public static byte[] inputStreamAsByteArray(final InputStream inputStream) {
    try (inputStream) {
      return inputStream.readAllBytes();
    } catch (final IOException e) {
      throw LOG.unableToReadInputStream(e);
    }
  }

  /**
   * 将 Reader 内容以字符串形式返回。
   *
   * @param reader Reader
   * @return Reader 内容对应的字符串
   * @deprecated 请勿使用。该方法将被移除。
   */
  @Deprecated(since = "1.1", forRemoval = true)
  public static String readerAsString(final Reader reader) {
    try (final StringWriter writer = new StringWriter()) {
      reader.transferTo(writer);
      return writer.toString();
    } catch (final IOException e) {
      throw LOG.unableToReadFromReader(e);
    } finally {
      closeSilently(reader);
    }
  }

  /**
   * 将字符串以输入流形式返回。
   *
   * @param string 要转换的字符串
   * @return 包含该字符串的输入流
   */
  public static InputStream stringAsInputStream(final String string) {
    return new ByteArrayInputStream(string.getBytes(ENCODING_CHARSET));
  }

  /**
   * 关闭可关闭对象，忽略任何 IO 异常。
   *
   * @param closeable 要关闭的可关闭对象
   */
  public static void closeSilently(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (final IOException e) {
      // 忽略
    }
  }

  /**
   * 返回指定文件名的文件内容
   *
   * @param filename 要加载的文件名
   * @return 文件内容对应的字符串
   */
  public static String fileAsString(final String filename) {
    final File classpathFile = getClasspathFile(filename);
    return fileAsString(classpathFile);
  }

  /**
   * 返回文件的内容。
   *
   * @param file 要加载的文件
   * @return 文件内容对应的字符串
   */
  public static String fileAsString(final File file) {
    try {
      return inputStreamAsString(new FileInputStream(file));
    } catch (final FileNotFoundException e) {
      throw LOG.fileNotFoundException(file.getAbsolutePath(), e);
    }
  }

  /**
   * 返回文件的内容。
   *
   * @param file 要加载的文件
   * @return 文件内容对应的字符串
   */
  public static byte[] fileAsByteArray(final File file) {
    try {
      return inputStreamAsByteArray(new FileInputStream(file));
    } catch (final FileNotFoundException e) {
      throw LOG.fileNotFoundException(file.getAbsolutePath(), e);
    }
  }

  /**
   * 返回指定文件名的文件输入流
   *
   * @param filename 要加载的文件名
   * @return 文件内容对应的输入流
   * @throws IoUtilException 若文件无法加载
   */
  public static InputStream fileAsStream(final String filename) {
    final File classpathFile = getClasspathFile(filename);
    return fileAsStream(classpathFile);
  }

  /**
   * 返回文件的输入流。
   *
   * @param file 要加载的文件
   * @return 文件内容对应的输入流
   * @throws IoUtilException 若文件无法加载
   */
  public static InputStream fileAsStream(final File file) {
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (final FileNotFoundException e) {
      throw LOG.fileNotFoundException(file.getAbsolutePath(), e);
    }
  }

  /**
   * 根据文件名返回对应的 File。
   *
   * @param filename 要加载的文件名
   * @return 文件对象
   */
  public static File getClasspathFile(final String filename) {
    if (filename == null) {
      throw LOG.nullParameter("filename");
    }

    return getClasspathFile(filename, null);
  }

  /**
   * 根据文件名返回对应的 File。
   *
   * @param filename 要加载的文件名
   * @param classLoader 用于加载文件的类加载器，若为 null 则回退到 TCCL，再回退到本类的类加载器
   * @return 文件对象
   * @throws IoUtilException 若文件无法加载
   */
  public static File getClasspathFile(final String filename, ClassLoader classLoader) {
    if (filename == null) {
      throw LOG.nullParameter("filename");
    }

    URL fileUrl = null;

    if (classLoader != null) {
      fileUrl = classLoader.getResource(filename);
    }
    if (fileUrl == null) {
      // 尝试当前线程的上下文类加载器
      classLoader = Thread.currentThread().getContextClassLoader();
      fileUrl = classLoader.getResource(filename);

      if (fileUrl == null) {
        // 最后尝试本类的类加载器
        classLoader = IoUtil.class.getClassLoader();
        fileUrl = classLoader.getResource(filename);
      }
    }

    if (fileUrl == null) {
      throw LOG.fileNotFoundException(filename);
    }

    try {
      return new File(fileUrl.toURI());
    } catch (final URISyntaxException e) {
      throw LOG.fileNotFoundException(filename, e);
    }
  }
}

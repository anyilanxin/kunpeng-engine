/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.engine.dmn.util;

import com.anyilanxin.kunpeng.engine.dmn.exception.IoUtilException;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Sebastian Menski
 */
public class IoUtil {
  private static final IoUtilLogger LOG = UtilsLogger.IO_UTIL_LOGGER;
  public static final Charset ENCODING_CHARSET = StandardCharsets.UTF_8;

  protected IoUtil() {}

  /**
   * Returns the input stream as String.
   *
   * @param inputStream the input stream
   * @return the input stream as String.
   */
  public static String inputStreamAsString(final InputStream inputStream) {
    return new String(inputStreamAsByteArray(inputStream), ENCODING_CHARSET);
  }

  /**
   * Returns the input stream as byte[].
   *
   * @param inputStream the input stream
   * @return the input stream as byte[].
   */
  public static byte[] inputStreamAsByteArray(final InputStream inputStream) {
    try (inputStream) {
      return inputStream.readAllBytes();
    } catch (final IOException e) {
      throw LOG.unableToReadInputStream(e);
    }
  }

  /**
   * Returns the Reader content as String.
   *
   * @param reader the Reader
   * @return the Reader content as String
   * @deprecated Do not use. This method will be removed.
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
   * Returns the String as InputStream.
   *
   * @param string the String to convert
   * @return the InputStream containing the String
   */
  public static InputStream stringAsInputStream(final String string) {
    return new ByteArrayInputStream(string.getBytes(ENCODING_CHARSET));
  }

  /**
   * Close a closable ignoring any IO exception.
   *
   * @param closeable the closable to close
   */
  public static void closeSilently(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (final IOException e) {
      // ignore
    }
  }

  /**
   * Returns the content of a file with specified filename
   *
   * @param filename name of the file to load
   * @return Content of the file as String
   */
  public static String fileAsString(final String filename) {
    final File classpathFile = getClasspathFile(filename);
    return fileAsString(classpathFile);
  }

  /**
   * Returns the content of a File.
   *
   * @param file the file to load
   * @return Content of the file as String
   */
  public static String fileAsString(final File file) {
    try {
      return inputStreamAsString(new FileInputStream(file));
    } catch (final FileNotFoundException e) {
      throw LOG.fileNotFoundException(file.getAbsolutePath(), e);
    }
  }

  /**
   * Returns the content of a File.
   *
   * @param file the file to load
   * @return Content of the file as String
   */
  public static byte[] fileAsByteArray(final File file) {
    try {
      return inputStreamAsByteArray(new FileInputStream(file));
    } catch (final FileNotFoundException e) {
      throw LOG.fileNotFoundException(file.getAbsolutePath(), e);
    }
  }

  /**
   * Returns the input stream of a file with specified filename
   *
   * @param filename the name of a File to load
   * @return the file content as input stream
   * @throws IoUtilException if the file cannot be loaded
   */
  public static InputStream fileAsStream(final String filename) {
    final File classpathFile = getClasspathFile(filename);
    return fileAsStream(classpathFile);
  }

  /**
   * Returns the input stream of a file.
   *
   * @param file the File to load
   * @return the file content as input stream
   * @throws IoUtilException if the file cannot be loaded
   */
  public static InputStream fileAsStream(final File file) {
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (final FileNotFoundException e) {
      throw LOG.fileNotFoundException(file.getAbsolutePath(), e);
    }
  }

  /**
   * Returns the File for a filename.
   *
   * @param filename the filename to load
   * @return the file object
   */
  public static File getClasspathFile(final String filename) {
    if (filename == null) {
      throw LOG.nullParameter("filename");
    }

    return getClasspathFile(filename, null);
  }

  /**
   * Returns the File for a filename.
   *
   * @param filename the filename to load
   * @param classLoader the classLoader to load file with, if null falls back to TCCL and then this
   *     class's classloader
   * @return the file object
   * @throws IoUtilException if the file cannot be loaded
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
      // Try the current Thread context classloader
      classLoader = Thread.currentThread().getContextClassLoader();
      fileUrl = classLoader.getResource(filename);

      if (fileUrl == null) {
        // Finally, try the classloader for this class
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

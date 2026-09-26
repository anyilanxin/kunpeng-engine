package com.anyilanxin.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

/**
 * base64与文件处理
 *
 * @author zxh
 * @date 2020-10-15 23:42
 * @since 1.0.0
 */
@Slf4j
public class Base64FileUtils {
  /**
   * base64转InputStream
   *
   * @param base64 ${@link String}
   * @return InputStream ${@link InputStream}
   * @author zxh
   * @date 2020-10-17 22:24
   */
  public static InputStream base64ToInputStream(final String base64) {
    final byte[] bytes = Base64.decodeBase64(base64);
    return new ByteArrayInputStream(bytes);
  }

  /**
   * 字符串转Base64字符串
   *
   * @param str ${@link String}
   * @return InputStream ${@link InputStream}
   * @author zxh
   * @date 2020-10-17 22:24
   */
  public static String strToBase64Str(final String str) {
    return Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * InputStream转base64
   *
   * @param inputStream ${@link InputStream}
   * @return String ${@link String}
   * @author zxh
   * @date 2020-10-17 22:30
   */
  public static String inputStreamToBase64(final InputStream inputStream) {
    byte[] bytes = new byte[0];
    try {
      bytes = IOUtils.toByteArray(inputStream);
    } catch (final IOException e) {
      e.printStackTrace();
      log.error(
          "------------Base64FileUtils------InputStream转byte[]异常------>inputStreamToBase64:{}",
          e.getMessage());
    }
    return Base64.encodeBase64String(bytes);
  }
}

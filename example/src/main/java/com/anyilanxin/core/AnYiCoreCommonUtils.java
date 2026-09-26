/*
 * Copyright (c) 2021-present ZHOUXUANHONG(安一老厨)<anyilanxin@aliyun.com>
 *
 * 本软件 AnYi Cloud EE 为 AnYi Cloud 的商业授权软件。未经过商业授权禁止使用，违者必究。
 *
 * AnYi Cloud EE 为商业授权软件，您在使用过程中，需要注意以下几点：
 *   1.不允许在国家法律法规规定的范围外使用，如出现违法行为作者本人不承担任何责任；
 *   2.软件使用的第三方依赖皆为开源软件，如需要修改第三方依赖请遵循第三方依赖附带的开源协议，因擅自修改第三方依赖所引起的争议，作者不承担任何责任；
 *   3.不得基于AnYi Cloud EE的基础，修改包装而成一个与AnYi Cloud EE、AnYi Zeebe EE、AnYi Standalone EE功能类似的程序，进行销售或发布，参与同类软件产品市场的竞争；
 *   4.不得将软件源码以任何开源方式公布出去；
 *   5.不得对授权进行出租、出售、抵押或发放子许可证；
 *   6.您可以直接使用在自己的网站或软件产品中，也可以集成到您自己的商业网站或软件产品中进行出租或销售；
 *   7.您可以对上述授权软件进行必要的修改和美化，无需公开修改或美化后的源代码；
 *   8.本软件流程部分请遵循camunda开源协议：
 *     https://docs.camunda.org/manual/latest/introduction/third-party-libraries
 *     https://github.com/camunda/camunda-bpm-platform/blob/master/LICENSE
 *   9.除满足上面条款外，在其他商业领域使用不受影响。同时作者为商业授权使用者在使用过程中出现的纠纷提供协助。
 */
package com.anyilanxin.core;

import com.google.common.base.CaseFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统工具类
 *
 * @author zxh
 * @date 2020-09-25 09:34
 * @since 1.0.0
 */
@Slf4j
public class AnYiCoreCommonUtils {

  /**
   * 文件大小格式化
   *
   * @param size ${@link Long} 文件大小
   * @return String ${@link String} 返回格式化的文件大小
   * @author zxh
   * @date 2020-10-23 14:47
   */
  public static String getFormatFileSize(final long size) {
    final String fileSize;
    final String unit;
    final DecimalFormat df = new DecimalFormat("#.00");
    if (size < (1 << 10)) {
      fileSize = df.format((double) size);
      unit = "B";
    } else if (size < (1 << 20)) {
      fileSize = df.format((double) size / (1 << 10));
      unit = "KB";
    } else if (size < (1 << 30)) {
      fileSize = df.format((double) size / (1 << 20));
      unit = "MB";
    } else {
      fileSize = df.format((double) size / (1 << 30));
      unit = "GB";
    }
    return fileSize + unit;
  }

  /**
   * 获取文件md5值
   *
   * @param inputStream ${@link InputStream}
   * @return String ${@link String}
   * @author zxh
   * @date 2020-10-23 14:50
   */
  public static String getFileMd5Hex(final InputStream inputStream) {
    String md5 = "";
    try {
      md5 = DigestUtils.md5Hex(inputStream);
    } catch (final IOException e) {
      log.error("------------AnYiCoreCommonUtils------------>getFileMd5Hex:{}", "获取文件md5值失败");
    }
    return md5;
  }

  /**
   * 获取堆栈信息
   *
   * @param throwable ${@link Throwable} 异常信息
   * @return String ${@link String} 处理结果
   * @author zxh
   * @date 2020-08-27 15:22
   */
  public static String getStackTrace(final Throwable throwable) {
    final StringWriter sw = new StringWriter();
    try (final PrintWriter pw = new PrintWriter(sw)) {
      throwable.printStackTrace(pw);
      return sw.toString();
    }
  }

  /**
   * 驼峰命名转下划线
   *
   * @param str ${@link String} 驼峰
   * @return String ${@link String} 下划线
   * @author zxh
   * @date 2019-08-01 13:54
   */
  public static String humpToUnderline(final String str) {
    if (StringUtils.isNotBlank(str)) {
      return CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(str);
    }
    return null;
  }

  /**
   * 下划线转驼峰
   *
   * @param str ${@link String}
   * @return String ${@link String}
   * @author zxh
   * @date 2019-08-01 13:58
   */
  public static String underlineToHump(final String str) {
    if (StringUtils.isNotBlank(str)) {
      return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str);
    }
    return null;
  }

  /**
   * 获取get中参数
   *
   * @param queryStr ${@link String} 待解析内容
   * @return Map<String, String> ${@link Map } 解析结果
   * @author zxh
   * @date 2021-01-08 14:36
   */
  public static Map<String, String> getQueryMap(final String queryStr) {
    final Map<String, String> queryMap = new HashMap<>(8);
    if (StringUtils.isNotBlank(queryStr)) {
      final String[] queryParam = queryStr.split("&");
      Arrays.stream(queryParam)
          .forEach(
              s -> {
                final String[] kv = s.split("=", 2);
                final String value = kv.length == 2 ? kv[1] : "";
                queryMap.put(kv[0], value);
              });
    }
    return queryMap;
  }

  /**
   * get参数转String
   *
   * @param queryMap ${@link Map} 待组装参数
   * @return String ${@linkString } 解析结果
   * @author zxh
   * @date 2021-01-08 14:36
   */
  public static String queryToString(final Map<String, String> queryMap) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (queryMap != null && !queryMap.isEmpty()) {
      queryMap.forEach((k, v) -> stringBuilder.append("&").append(k).append("=").append(v));
    }
    return stringBuilder.toString().replaceFirst("&", "");
  }

  /**
   * 获取SpringBootApplication扫描路径或某个类包路径
   *
   * @param clas ${@link Class}
   * @return String[] ${@link String[]}
   * @author zxh
   * @date 2021-04-12 15:29
   */
  public static <T> String[] getPackages(final Class<T> clas) {
    final SpringBootApplication bootApplication = clas.getAnnotation(SpringBootApplication.class);
    String[] packages = null;
    if (Objects.nonNull(bootApplication)) {
      packages = bootApplication.scanBasePackages();
    }
    if (ArrayUtils.getLength(packages) <= 0 && Objects.nonNull(clas)) {
      packages = new String[] {clas.getPackage().getName()};
    }
    return packages;
  }

  /**
   * url中获取uri
   *
   * @param url ${@link String} url信息
   * @return String ${@link String}
   * @author zxh
   * @date 2021-07-11 23:28
   */
  public static String getUri(final String url) {
    if (StringUtils.isBlank(url)) {
      return "";
    } else {
      return url.replaceAll("^(http(s?)://)([a-zA-Z0-9]+)(:\\d+)?", "");
    }
  }

  /**
   * 获取32位uuid(使用ThreadLocalRandom提高性能)
   *
   * @return String ${@link String}
   * @author zxh
   * @date 2019-04-01 17:02
   */
  public static String get32UUId() {
    final ThreadLocalRandom random = ThreadLocalRandom.current();
    return new UUID(random.nextLong(), random.nextLong()).toString().replace("-", "");
  }

  /** 中文数字 */
  private static final String[] CN_NUM = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

  /** 中文数字单位 */
  private static final String[] CN_UNIT = {
    "", "十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千"
  };

  /** 特殊字符：负 */
  private static final String CN_NEGATIVE = "负";

  /** 特殊字符：点 */
  private static final String CN_POINT = "点";

  /**
   * int 转 中文数字 支持到int最大值
   *
   * @param intNum 要转换的整型数
   * @return 中文数字
   */
  public static String int2chineseNum(int intNum) {
    final StringBuilder sb = new StringBuilder();
    boolean isNegative = false;
    if (intNum < 0) {
      isNegative = true;
      intNum *= -1;
    }
    int count = 0;
    while (intNum > 0) {
      sb.insert(0, CN_NUM[intNum % 10] + CN_UNIT[count]);
      intNum = intNum / 10;
      count++;
    }

    if (isNegative) {
      sb.insert(0, CN_NEGATIVE);
    }

    return sb.toString()
        .replaceAll("零[千百十]", "零")
        .replaceAll("零+万", "万")
        .replaceAll("零+亿", "亿")
        .replaceAll("亿万", "亿零")
        .replaceAll("零+", "零")
        .replaceAll("零$", "");
  }

  /**
   * bigDecimal 转 中文数字 整数部分只支持到int的最大值
   *
   * @param bigDecimalNum 要转换的BigDecimal数
   * @return 中文数字
   */
  public static String bigDecimal2chineseNum(final BigDecimal bigDecimalNum) {
    if (bigDecimalNum == null) {
      return CN_NUM[0];
    }

    final StringBuilder sb = new StringBuilder();

    // 将小数点后面的零给去除
    final String numStr = bigDecimalNum.abs().stripTrailingZeros().toPlainString();

    final String[] split = numStr.split("\\.");
    final String integerStr = int2chineseNum(Integer.parseInt(split[0]));

    sb.append(integerStr);

    // 如果传入的数有小数，则进行切割，将整数与小数部分分离
    if (split.length == 2) {
      // 有小数部分
      sb.append(CN_POINT);
      final String decimalStr = split[1];
      final char[] chars = decimalStr.toCharArray();
      for (final char aChar : chars) {
        final int index = Integer.parseInt(String.valueOf(aChar));
        sb.append(CN_NUM[index]);
      }
    }

    // 判断传入数字为正数还是负数
    final int signum = bigDecimalNum.signum();
    if (signum == -1) {
      sb.insert(0, CN_NEGATIVE);
    }

    return sb.toString();
  }
}

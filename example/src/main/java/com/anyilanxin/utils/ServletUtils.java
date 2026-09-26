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
package com.anyilanxin.utils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * core mvc servlet util
 *
 * @author zxh
 * @date 2020-10-07 09:24
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServletUtils {
  private static final String UNKNOWN = "unknown";
  private static ServletUtils utils;
  private final HttpServletRequest request;

  /** 获取request */
  public static HttpServletRequest getRequest() {
    return Objects.nonNull(getRequestAttributes()) ? getRequestAttributes().getRequest() : null;
  }

  /** 获取response */
  public static HttpServletResponse getResponse() {
    return Objects.nonNull(getRequestAttributes()) ? getRequestAttributes().getResponse() : null;
  }

  /** 获取session */
  public static HttpSession getSession() {
    return Objects.nonNull(getRequest()) ? getRequest().getSession() : null;
  }

  public static ServletRequestAttributes getRequestAttributes() {
    try {
      final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
      return (ServletRequestAttributes) attributes;
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * 是否是Ajax异步请求
   *
   * @param request ${@link HttpServletRequest}
   * @author zxh
   * @date 2021-01-13 01:53
   */
  public static boolean isAjaxRequest(final HttpServletRequest request) {
    if (Objects.nonNull(request)) {
      final String contentType = request.getContentType();
      if (StringUtils.isNotBlank(contentType)
          && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
        return true;
      }
    }
    return false;
  }

  /** 获取IP地址 */
  public static String getIpAddr(final HttpServletRequest request) {
    String ip = null;
    try {
      ip = request.getHeader("x-forwarded-for");
      if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
        ip = request.getHeader("Proxy-Client-IP");
      }
      if (StringUtils.isEmpty(ip) || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
        ip = request.getHeader("WL-Proxy-Client-IP");
      }
      if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
        ip = request.getHeader("HTTP_CLIENT_IP");
      }
      if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
      }
      if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
      }
    } catch (final Exception e) {
      log.debug("------------IPUtils------IPUtils ERROR------>getIpAddr:{}", e);
    }
    return ip;
  }

  /**
   * 获取ip信息
   *
   * @return String ${@link String} ip信息
   * @author zxh
   * @date 2020-10-22 15:08
   */
  public static String getIpAddr() {
    return getIpAddr(utils.request);
  }

  /**
   * 获取浏览器user agent信息
   *
   * @return String ${@link String}
   * @author zxh
   * @date 2020-11-02 12:10
   */
  public static String getUserAgent() {
    return getUserAgent(utils.request);
  }

  /**
   * 获取浏览器user agent信息
   *
   * @param request ${@link HttpServletRequest}
   * @return String ${@link String}
   * @author zxh
   * @date 2020-11-02 12:10
   */
  public static String getUserAgent(final HttpServletRequest request) {
    return request.getHeader("User-Agent");
  }

  @PostConstruct
  void init() {
    utils = this;
  }
}

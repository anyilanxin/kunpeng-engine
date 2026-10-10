/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

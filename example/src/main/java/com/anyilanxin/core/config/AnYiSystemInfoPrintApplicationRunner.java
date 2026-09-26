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
package com.anyilanxin.core.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 打印启动信息
 *
 * @author zhou
 * @date 2022-07-17 23:03
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnYiSystemInfoPrintApplicationRunner implements ApplicationRunner {
  private final Environment environment;

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    String ip = "localhost";
    try {
      ip = InetAddress.getLocalHost().getHostAddress();
    } catch (final UnknownHostException e) {
      log.error("------------------发生了错误------run--->{}", e.getMessage());
      e.printStackTrace();
    }
    final String port = environment.getProperty("server.port");
    // 先主动判断一次是不是webflux
    boolean webflux = true;
    String path = environment.getProperty("spring.webflux.base-path", "");
    if (StringUtils.isBlank(path)) {
      webflux = false;
      path = environment.getProperty("server.servlet.context-path", "");
    }
    // 最后再判断是否配置设置的为webflux
    if (Boolean.TRUE.equals(environment.getProperty("springdoc.webflux", Boolean.class))) {
      webflux = true;
      path = environment.getProperty("spring.webflux.base-path", "");
    }
    String swaggerUrl = environment.getProperty("springdoc.swagger-ui.path");
    final String customUrl = environment.getProperty("springdoc.swagger-ui.custom-path");
    if (StringUtils.isNotBlank(customUrl)) {
      swaggerUrl = customUrl;
    }
    if (StringUtils.isBlank(swaggerUrl)) {
      if (webflux) {
        swaggerUrl = path + "/webjars/swagger-ui/index.html";
      } else {
        swaggerUrl = path + "/swagger-ui/index.html";
      }
    }
    final String profilesActive = environment.getProperty("spring.profiles.active");
    final String version = environment.getProperty("spring.application.version");
    final String projectName = environment.getProperty("spring.application.name");
    final String template =
        """


                =======================================================================================================================

                    AnYi Cloud Enterprise Application（%s v%s %s）is running! Access URLs:

                           ___      __  ___    __               _  __ _          ____________
                          /   |  ___\\ \\/ (_)  / /   ____ _____ | |/ /(_)___     / ____/ ____/
                         / /| | / __ \\  / /  / /   / __ `/ __ \\|   // / __ \\   / __/ / __/
                        / ___ |/ / / / / /  / /___/ /_/ / / / /   |/ / / / /  / /___/ /___
                       /_/  |_/_/ /_/_/_/  /_____/\\__,_/_/ /_/_/|_/_/_/ /_/  /_____/_____/

                        安一兰心(AN YI LAN XIN)


                    Website Preview:\thttps://anyilanxin.com
                    Api Url  Prefix:\thttp://%s:%s%s
                    Spring  Doc  Ui:\thttp://%s:%s%s

                =======================================================================================================================
                """;
    final String info =
        template.formatted(
            projectName,
            version,
            StringUtils.isNotBlank(profilesActive) ? profilesActive : "",
            ip,
            port,
            path,
            ip,
            port,
            swaggerUrl);
    log.info(info);
    // @formatter:off
  }
}

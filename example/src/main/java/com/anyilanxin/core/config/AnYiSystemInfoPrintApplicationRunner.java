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

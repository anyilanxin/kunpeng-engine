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
package com.anyilanxin.core.config.properties;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * app节点配置信息
 *
 * @author zxh
 * @date 2020-06-29 00:25
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "app")
public class CoreWebMvcProperty implements Serializable {
  @Serial private static final long serialVersionUID = 713575253040294540L;

  /** 运行环境 */
  @Value("${spring.profiles.active:dev}")
  private String active = "dev";

  /** 服务名称 */
  @Value("${spring.application.name:anyi}")
  private String serviceName;

  /** 当前配置文件路径 */
  @Value(value = "classpath:application-${spring.profiles.active:dev}.yml")
  private Resource resource;

  /** 请求前缀 */
  @Value("${server.servlet.context-path:/}")
  private String contentPath;

  /** 请求端口 */
  @Value("${server.port:8080}")
  private int port;

  /** 服务器ip */
  @Value("${spring.cloud.nacos.discovery.ip:}")
  private String ip;

  /** 是否生成外置配置文件 */
  private boolean createOutConf = false;

  public String getIp() {
    if (StringUtils.isBlank(ip)) {
      String machineIp = "localhost";
      try {
        machineIp = InetAddress.getLocalHost().getHostAddress();
      } catch (final UnknownHostException ignored) {
      }
      ip = machineIp;
    }
    return ip;
  }
}

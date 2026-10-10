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

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * spring doc配置
 *
 * @author zxh
 * @date 2020-08-30 15:33
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "springdoc")
public class SpringDocCoreMvcProperty {

  /** 请求头 */
  private Set<String> headers;

  /** 是否为webflux,用于spring doc打印信息路径判断使用 */
  private boolean webflux;

  /** 版本号 */
  private String version;

  /** swagger请求前缀 */
  private String apiPrefix;

  /** 联系人 */
  private String contactUser = "zxh";

  /** 联系邮箱 */
  private String contactEmail = "";

  /** 标题 */
  private String title = "";

  /** oauth2授权地址前缀 */
  private String auth2Prefix = "http://127.0.0.1/authorization";

  /** 扫包路径 */
  private String packagesToScan;

  public Set<String> getHeaders() {
    if (headers == null || headers.isEmpty()) {
      headers = new HashSet<>();
    }
    return headers;
  }
}

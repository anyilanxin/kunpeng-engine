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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.configuration.gateway;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * gateway Web 控制台配置（{@link GatewayCfg} 内嵌实体，yaml 键随宿主形态：内嵌网关 {@code
 * kunpeng.broker.gateway.webapp.*}、独立网关 {@code kunpeng.gateway.webapp.*}）：用户在配置文件声明（密码支持
 * {noop}/{bcrypt} 等前缀格式，缺省 {noop} 明文），未配置用户时不可登录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GatewayWebappCfg {

  /** Web 控制台总开关，关闭时安全链与 API 均不装配 */
  private boolean enabled = true;

  /** 控制台底栏备案信息（如 ICP 备案号），空则前端不显示 */
  private String filingInfo;

  /** 控制台用户清单 */
  private List<GatewayWebappUser> users = new ArrayList<>();

  /** 控制台用户：账号、密码（密码编码前缀格式）、角色集 */
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  public static class GatewayWebappUser {
    private String username;
    private String password;
    private Set<String> roles = new LinkedHashSet<>();
  }
}

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
package com.anyilanxin.kunpeng.gateway.webapp;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 控制台配置：用户在后台配置文件声明（密码支持 {noop}/{bcrypt} 等前缀格式，缺省 {noop} 明文），未配置用户时不可登录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@ConfigurationProperties(prefix = "kunpeng.gateway-webapp")
public class WebappProperties {

  /** Web 控制台总开关，关闭时安全链与 API 均不装配 */
  private boolean enabled = true;

  /** 控制台用户清单 */
  private List<WebappUser> users = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public List<WebappUser> getUsers() {
    return users;
  }

  public void setUsers(final List<WebappUser> users) {
    this.users = users;
  }

  /** 控制台用户：账号、密码（密码编码前缀格式）、角色集 */
  public static class WebappUser {
    private String username;
    private String password;
    private Set<String> roles = new LinkedHashSet<>();

    public String getUsername() {
      return username;
    }

    public void setUsername(final String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(final String password) {
      this.password = password;
    }

    public Set<String> getRoles() {
      return roles;
    }

    public void setRoles(final Set<String> roles) {
      this.roles = roles;
    }
  }
}

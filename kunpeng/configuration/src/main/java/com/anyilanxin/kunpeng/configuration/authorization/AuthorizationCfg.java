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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.configuration.authorization;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎授权校验独立配置实体（yaml 键 {@code kunpeng.broker.authorization.*}，dist broker/restore profile 经
 * Properties 子类注册）：渐进启用开关 + admin 白名单（用户/组 id）。默认关闭，灰度验证后转强制；后续授权相关扩展 （插件链、claim/resolve
 * 规则、资源级权限）集中定义在本实体。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AuthorizationCfg {
  private boolean enabled = false;
  private List<String> adminUsers = new ArrayList<>();
  private List<String> adminGroups = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getAdminUsers() {
    return adminUsers;
  }

  public void setAdminUsers(final List<String> adminUsers) {
    this.adminUsers = adminUsers;
  }

  public List<String> getAdminGroups() {
    return adminGroups;
  }

  public void setAdminGroups(final List<String> adminGroups) {
    this.adminGroups = adminGroups;
  }

  @Override
  public String toString() {
    return "AuthorizationCfg{enabled="
        + enabled
        + ", adminUsers="
        + adminUsers
        + ", adminGroups="
        + adminGroups
        + '}';
  }
}

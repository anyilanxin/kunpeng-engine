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
package com.anyilanxin.kunpeng.client.spring.annotation.value;

import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import java.util.List;
import java.util.Objects;

/**
 * 部署注解值信息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DeploymentValue implements KunpengAnnotationValue<ClassInfo> {

  private final List<String> resources;

  private final ClassInfo beanInfo;

  private DeploymentValue(final List<String> resources, final ClassInfo beanInfo) {
    this.resources = resources;
    this.beanInfo = beanInfo;
  }

  public List<String> getResources() {
    return resources;
  }

  @Override
  public ClassInfo getBeanInfo() {
    return beanInfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(resources, beanInfo);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final DeploymentValue that = (DeploymentValue) o;
    return Objects.equals(resources, that.resources) && Objects.equals(beanInfo, that.beanInfo);
  }

  @Override
  public String toString() {
    return "DeploymentValue{" + "resources=" + resources + ", beanInfo=" + beanInfo + '}';
  }

  public static DeploymentValueBuilder builder() {
    return new DeploymentValueBuilder();
  }

  public static final class DeploymentValueBuilder {

    private List<String> resources;
    private ClassInfo beanInfo;

    private DeploymentValueBuilder() {}

    public DeploymentValueBuilder resources(final List<String> resources) {
      this.resources = resources;
      return this;
    }

    public DeploymentValueBuilder beanInfo(final ClassInfo beanInfo) {
      this.beanInfo = beanInfo;
      return this;
    }

    public DeploymentValue build() {
      return new DeploymentValue(resources, beanInfo);
    }
  }
}

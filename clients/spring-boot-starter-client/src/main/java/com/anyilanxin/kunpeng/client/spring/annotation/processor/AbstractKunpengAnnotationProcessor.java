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
package com.anyilanxin.kunpeng.client.spring.annotation.processor;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 注解处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class AbstractKunpengAnnotationProcessor
    implements ApplicationContextAware, KunpengClientLifecycleAware {
  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  protected abstract boolean isApplicableFor(ClassInfo beanInfo);

  protected abstract void configureFor(final ClassInfo beanInfo);

  protected abstract void start(KunpengClient client);

  protected abstract void stop(KunpengClient client);

  @Override
  public void onStart(final KunpengClient client) {
    for (final String beanName : applicationContext.getBeanDefinitionNames()) {
      final Object bean = applicationContext.getBean(beanName);
      final ClassInfo classInfo = ClassInfo.builder().beanName(beanName).bean(bean).build();
      if (isApplicableFor(classInfo)) {
        configureFor(classInfo);
      }
    }
    start(client);
  }

  @Override
  public void onStop(final KunpengClient client) {
    stop(client);
  }
}

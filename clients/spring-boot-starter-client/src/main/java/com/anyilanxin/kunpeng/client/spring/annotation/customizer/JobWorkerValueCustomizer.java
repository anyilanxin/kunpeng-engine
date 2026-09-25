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
package com.anyilanxin.kunpeng.client.spring.annotation.customizer;

import com.anyilanxin.kunpeng.client.spring.annotation.JobWorker;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.properties.PropertyBasedJobWorkerValueCustomizer;

/**
 * This interface could be used to customize the {@link JobWorker} annotation's values. Just
 * implement it and put it into to the {@link org.springframework.context.ApplicationContext} to
 * make it work. But be careful: these customizers are applied sequentially and if you need to
 * change the order of these customizers use the {@link org.springframework.core.annotation.Order}
 * annotation or the {@link org.springframework.core.Ordered} interface.
 *
 * @see PropertyBasedJobWorkerValueCustomizer
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobWorkerValueCustomizer {

  void customize(final JobWorkerValue jobWorkerValue);
}

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
package com.anyilanxin.kunpeng.client.spring.jobhandling.result;

import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;

/**
 * 结果处理上下文。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ResultProcessorContext {
  private final Object result;
  private final ActivatedJob job;

  public ResultProcessorContext(final Object result, final ActivatedJob job) {
    this.result = result;
    this.job = job;
  }

  public Object getResult() {
    return result;
  }

  public ActivatedJob getJob() {
    return job;
  }
}

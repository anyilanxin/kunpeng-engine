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
package com.anyilanxin.kunpeng.gateway.rest.service.vo;

import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.PullResponse;
import java.util.List;

/**
 * 长轮询拉取结果：空批表示 deadline 到期（无任务可拉，不是失败）。
 *
 * @param jobs 拉取到的 job 列表
 * @author zxuanhong
 * @since 2026.9.0
 */
public record PullVo(List<JobVo> jobs) {

  public static PullVo of(final PullResponse response) {
    return new PullVo(response.getJobsList().stream().map(JobVo::of).toList());
  }
}

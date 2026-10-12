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
package com.anyilanxin.kunpeng.gateway.rest.controller;

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.JobCompleteDto;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.JobFailDto;
import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.JobService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.JobVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.PullVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * job 接口：命令面（完成/失败）与消费面（SSE 推送流、长轮询拉取）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@Validated
@RequestMapping(
    value = "/{version}/jobs",
    version = "v1",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Job", description = "job 命令与消费（SSE 推送 / 长轮询）")
public class JobController {
  private final JobService jobService;

  public JobController(final JobService jobService) {
    this.jobService = jobService;
  }

  @PostMapping("/{jobKey}/complete")
  @Operation(summary = "完成 job")
  public Mono<Result<Void>> complete(
      @PathVariable final long jobKey, @Valid @RequestBody final JobCompleteDto dto) {
    return jobService.complete(jobKey, dto).thenReturn(Result.ok());
  }

  @PostMapping("/{jobKey}/fail")
  @Operation(summary = "标记 job 失败")
  public Mono<Result<Void>> fail(
      @PathVariable final long jobKey, @Valid @RequestBody final JobFailDto dto) {
    return jobService.fail(jobKey, dto).thenReturn(Result.ok());
  }

  @PutMapping("/{jobKey}/retries")
  @Operation(summary = "修改 job 重试次数（未实现）")
  public Mono<Result<Void>> updateRetries(
      @PathVariable final long jobKey, @RequestParam final int retries) {
    return jobService.updateRetries(jobKey, retries).thenReturn(Result.ok());
  }

  @PostMapping("/{jobKey}/error")
  @Operation(summary = "对 job 抛出业务错误（未实现）")
  public Mono<Result<Void>> throwError(
      @PathVariable final long jobKey,
      @RequestParam final String errorCode,
      @RequestParam(required = false) final String errorMessage) {
    return jobService.throwError(jobKey, errorCode, errorMessage).thenReturn(Result.ok());
  }

  @PutMapping("/{jobKey}/timeout")
  @Operation(summary = "修改 job 处理超时（未实现）")
  public Mono<Result<Void>> updateTimeout(
      @PathVariable final long jobKey, @RequestParam final long timeout) {
    return jobService.updateTimeout(jobKey, timeout).thenReturn(Result.ok());
  }

  @GetMapping("/pull")
  @Operation(summary = "长轮询拉取 job（空批=窗口到期无任务）")
  public Mono<Result<PullVo>> pull(
      @RequestParam @NotBlank(message = "消费类型不能为空") final String type,
      @RequestParam(defaultValue = "") final String worker,
      @RequestParam(defaultValue = "10") @Min(value = 1, message = "maxBatch 至少为 1") final int maxBatch,
      @RequestParam(defaultValue = "0")
          @Min(value = 0, message = "deadlineMs 不能为负") @Max(value = 300_000, message = "deadlineMs 上限 5 分钟") final long deadlineMs,
      @RequestParam(required = false) final List<String> tenantIds) {
    return jobService
        .pull(type, worker, maxBatch, deadlineMs, tenantIds == null ? List.of() : tenantIds)
        .map(Result::ok);
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "SSE 推送流订阅 job（event:job，注释帧保活）")
  public Flux<ServerSentEvent<JobVo>> stream(
      @RequestParam @NotBlank(message = "消费类型不能为空") final String jobType,
      @RequestParam(defaultValue = "") final String worker,
      @RequestParam(defaultValue = "0") final int capacity,
      @RequestParam(required = false) final List<String> tenantIds,
      @RequestParam(required = false) final List<String> fetchVariables,
      @RequestParam(defaultValue = "30000") @Min(value = 1000, message = "heartbeatMs 至少 1000ms") final long heartbeatMs) {
    return jobService.stream(
        jobType,
        worker,
        capacity,
        tenantIds == null ? List.of() : tenantIds,
        fetchVariables == null ? List.of() : fetchVariables,
        heartbeatMs);
  }
}

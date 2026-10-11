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

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.UserTaskCompleteDto;
import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.UserTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 用户任务接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(
    value = "/{version}/user-tasks",
    version = "v1",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "用户任务", description = "用户任务完成与取消")
public class UserTaskController {
  private final UserTaskService userTaskService;

  public UserTaskController(final UserTaskService userTaskService) {
    this.userTaskService = userTaskService;
  }

  @PostMapping("/{taskKey}/complete")
  @Operation(summary = "完成用户任务")
  public Mono<Result<Void>> complete(
      @PathVariable final long taskKey, @Valid @RequestBody final UserTaskCompleteDto dto) {
    return userTaskService.complete(taskKey, dto).thenReturn(Result.ok());
  }

  @DeleteMapping("/{taskKey}")
  @Operation(summary = "取消用户任务")
  public Mono<Result<Void>> cancel(@PathVariable final long taskKey) {
    return userTaskService.cancel(taskKey).thenReturn(Result.ok());
  }
}

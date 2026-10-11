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

import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 事件（incident）接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(value = "/v1/incidents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "事件", description = "事件解决")
public class IncidentController {
  private final IncidentService incidentService;

  public IncidentController(final IncidentService incidentService) {
    this.incidentService = incidentService;
  }

  @PostMapping("/{incidentKey}/resolve")
  @Operation(
      summary = "解决事件",
      tags = {"v1.0.0"})
  public Mono<Result<Void>> resolve(
      @PathVariable final long incidentKey, @RequestParam(required = false) final String tenantId) {
    return incidentService.resolve(incidentKey, tenantId).thenReturn(Result.ok());
  }
}

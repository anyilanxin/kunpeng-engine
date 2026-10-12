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
package com.anyilanxin.kunpeng.gateway.rest.core;

import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

/**
 * REST 全局异常处理：统一转 Result 包装（HTTP 状态保持 200，失败语义在 code 上），异常原样透出不做兜底改写。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_REST;

  @ExceptionHandler({WebExchangeBindException.class, ConstraintViolationException.class})
  public Mono<ResponseEntity<Result<Void>>> handleValidation(final Exception exception) {
    return Mono.just(ResponseEntity.ok(Result.fail(400, "参数校验失败：" + exception.getMessage())));
  }

  /** 请求体超出 codec 缓冲上限（spring.codec.max-in-memory-size）：归 413，提示上限值可调 */
  @ExceptionHandler(DataBufferLimitException.class)
  public Mono<ResponseEntity<Result<Void>>> handlePayloadTooLarge(
      final DataBufferLimitException exception) {
    return Mono.just(ResponseEntity.ok(Result.fail(413, "请求体超过网关缓冲上限：" + exception.getMessage())));
  }

  @ExceptionHandler(NotImplementedException.class)
  public Mono<ResponseEntity<Result<Void>>> handleNotImplemented(
      final NotImplementedException exception) {
    return Mono.just(ResponseEntity.ok(Result.fail(501, exception.getMessage())));
  }

  @ExceptionHandler(BrokerCommandException.class)
  public Mono<ResponseEntity<Result<Void>>> handleBrokerReject(
      final BrokerCommandException exception) {
    return Mono.just(ResponseEntity.ok(Result.fail(exception.getCode(), exception.getMessage())));
  }

  /** 客户端入参错误：消息面向调用方可读，归 400 */
  @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
  public Mono<ResponseEntity<Result<Void>>> handleClientError(final RuntimeException exception) {
    return Mono.just(
        ResponseEntity.ok(
            Result.fail(
                400,
                exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage())));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Result<Void>>> handleUnexpected(final Exception exception) {
    LOG.error("Rest request failed", exception);
    // 内部异常细节只进日志，对外只暴露类型不泄露消息
    return Mono.just(
        ResponseEntity.ok(Result.fail(500, "服务内部错误：" + exception.getClass().getSimpleName())));
  }
}

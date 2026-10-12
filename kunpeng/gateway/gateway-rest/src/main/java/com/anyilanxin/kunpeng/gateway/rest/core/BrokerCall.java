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

import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.ResponseRecordValue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * service 层共用的 broker 命令响应封装：future 由 {@link
 * com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient} 的具体方法给出（类型已钉死），
 * 此处只做成功取值/拒绝转 {@link BrokerCommandException} 的响应式适配。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BrokerCall {

  private BrokerCall() {}

  /**
   * 取携带数据的响应值并映射为目标结果（映射后归还响应缓冲）。
   *
   * @param future 命令响应 future
   * @param mapper 响应值到结果的映射
   * @param <Value> 响应记录类型
   * @param <Target> 映射结果类型
   * @return 映射结果；broker 拒绝时以 {@link BrokerCommandException} 失败
   */
  public static <Value extends ResponseRecordValue, Target> Mono<Target> awaitValue(
      final CompletableFuture<BrokerResponse<Value>> future, final Function<Value, Target> mapper) {
    return Mono.fromFuture(future)
        .flatMap(
            response -> {
              if (response.isSuccess()) {
                final Value value = response.getValue();
                try {
                  return Mono.just(mapper.apply(value));
                } finally {
                  value.reset();
                }
              }
              return Mono.error(new BrokerCommandException(response.code(), response.message()));
            });
  }

  /**
   * 仅回执（无数据载荷）的命令：成功即完成，响应值不读取但仍归还缓冲。
   *
   * @param future 命令响应 future
   * @param <Value> 响应记录类型
   * @return 完成信号；broker 拒绝时以 {@link BrokerCommandException} 失败
   */
  public static <Value extends ResponseRecordValue> Mono<Void> awaitAck(
      final CompletableFuture<BrokerResponse<Value>> future) {
    return Mono.fromFuture(future)
        .flatMap(
            response -> {
              if (response.isSuccess()) {
                response.getValue().reset();
                return Mono.empty();
              }
              return Mono.error(new BrokerCommandException(response.code(), response.message()));
            });
  }
}

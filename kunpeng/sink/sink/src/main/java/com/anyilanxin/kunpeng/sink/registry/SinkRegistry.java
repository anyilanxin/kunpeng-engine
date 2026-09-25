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
package com.anyilanxin.kunpeng.sink.registry;

import com.anyilanxin.kunpeng.cluster.utils.VisibleForTesting;
import com.anyilanxin.kunpeng.configuration.broker.SinkCfg;
import com.anyilanxin.kunpeng.sink.SinkLoggers;
import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.sink.runtime.SinkRuntimeContext;
import com.anyilanxin.kunpeng.utils.jar.ExternalJarLoadException;
import com.anyilanxin.kunpeng.utils.jar.ExternalJarRepository;
import com.anyilanxin.kunpeng.utils.jar.ThreadContextUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.InstantSource;
import java.util.HashMap;
import java.util.Map;
import org.agrona.CloseHelper;
import org.slf4j.Logger;

/**
 * 引擎可用的 Sink 集合，按 id 索引。
 *
 * <p>Sink 有两个来源：引擎类路径上的类，或部署在引擎旁边、按路径引用的 jar。 每个 Sink 在被登记进来之前都会先校验一次——用一次性上下文执行其 {@code
 * initialize} 回调—— 这样有问题的 Sink 会在启动阶段失败，而不是稍后在活着的分区上失败。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkRegistry {

  private static final Logger LOG = SinkLoggers.SINK;

  private final Map<String, SinkDescriptor> sinks = new HashMap<>();
  private final ExternalJarRepository jarRepository;

  public SinkRegistry() {
    this(new ExternalJarRepository());
  }

  public SinkRegistry(final ExternalJarRepository jarRepository) {
    this.jarRepository = jarRepository;
  }

  /**
   * @return 当前已登记的描述器，按 Sink id 索引
   */
  public Map<String, SinkDescriptor> getSinks() {
    return Map.copyOf(sinks);
  }

  /**
   * 登记 {@code config} 描述、以 {@code id} 为键的 Sink 。
   *
   * <p>若该 id 已存在，则原样返回已有描述器并忽略传入配置——同一 id 以首次登记为准。
   *
   * @param id 配置中的 Sink id
   * @param config 指明 Sink 类（以及可选 jar 路径）的 broker 配置项
   * @return 该 id 对应的描述器
   * @throws SinkLoadException 找不到类或校验失败时抛出
   * @throws ExternalJarLoadException 引用的外部 jar 无法加载时抛出
   */
  public SinkDescriptor load(final String id, final SinkCfg config)
      throws SinkLoadException, ExternalJarLoadException {
    final var existing = sinks.get(id);
    if (existing != null) {
      return existing;
    }

    final ClassLoader classLoader =
        config.isExternal() ? jarRepository.load(config.getJarPath()) : getClass().getClassLoader();
    final Class<? extends RecordSink> sinkClass = loadSinkClass(id, classLoader, config);

    return validateAndRegister(id, sinkClass, config.getArgs());
  }

  /**
   * 编程式登记一个 Sink ，登记前先校验；供引擎注册内置 Sink （如调试 Sink）使用。
   *
   * @param id Sink id
   * @param sinkClass 实现该 Sink 的类
   * @param arguments 原始配置参数
   * @return 该 id 对应的描述器
   * @throws SinkLoadException 校验失败时抛出
   */
  @VisibleForTesting
  public SinkDescriptor validateAndRegister(
      final String id,
      final Class<? extends RecordSink> sinkClass,
      final Map<String, Object> arguments)
      throws SinkLoadException {
    final var existing = sinks.get(id);
    if (existing != null) {
      return existing;
    }

    final var descriptor = new SinkDescriptor(id, sinkClass, arguments);
    validate(descriptor);
    sinks.put(id, descriptor);
    return descriptor;
  }

  private Class<? extends RecordSink> loadSinkClass(
      final String id, final ClassLoader classLoader, final SinkCfg config)
      throws SinkLoadException {
    try {
      return classLoader.loadClass(config.getClassName()).asSubclass(RecordSink.class);
    } catch (final ClassNotFoundException e) {
      throw new SinkLoadException(id, "class " + config.getClassName() + " not found", e);
    } catch (final ClassCastException e) {
      throw new SinkLoadException(
          id, config.getClassName() + " does not implement " + RecordSink.class.getName(), e);
    }
  }

  private void validate(final SinkDescriptor descriptor) throws SinkLoadException {
    final RecordSink instance = descriptor.newInstance();
    try {
      final SinkContext validationContext =
          new SinkRuntimeContext(
              SinkLoggers.forSink(descriptor.getId()),
              descriptor.getConfig(),
              SinkContext.PARTITION_ID_UNSET,
              new SimpleMeterRegistry(),
              InstantSource.system());
      ThreadContextUtil.runCheckedWithClassLoader(
          () -> instance.initialize(validationContext), instance.getClass().getClassLoader());
    } catch (final Exception e) {
      throw new SinkLoadException(descriptor.getId(), "configuration validation failed", e);
    } finally {
      CloseHelper.close(
          error ->
              LOG.warn(
                  "Ignoring failure while closing validation instance of sink '{}'",
                  descriptor.getId(),
                  error),
          instance::close);
    }
  }
}

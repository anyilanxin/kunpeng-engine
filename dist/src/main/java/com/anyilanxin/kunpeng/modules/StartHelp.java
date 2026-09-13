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
package com.anyilanxin.kunpeng.modules;

import com.anyilanxin.kunpeng.modules.broker.BrokerModuleConfiguration;
import com.anyilanxin.kunpeng.modules.broker.health.BrokerHealthConfigurationInitializer;
import com.anyilanxin.kunpeng.modules.gateway.GatewayModuleConfiguration;
import com.anyilanxin.kunpeng.modules.gateway.health.GatewayHealthConfigurationInitializer;
import com.anyilanxin.kunpeng.utils.error.FatalErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * start help
 *
 * @author zxuanhong
 * @since
 */
public class StartHelp {
  public static final Logger LOGGER = LoggerFactory.getLogger("com.anyilanxin.kunpeng");
  private final SpringApplication application;

  private StartHelp(final String[] args) {
    application = createSpringApplication(args);
  }

  public static StartHelp builder(final String[] args) {
    setDefaultGlobalConfiguration(args);
    return new StartHelp(args);
  }

  private static SpringApplication createSpringApplication(final String[] args) {
    final StartType startType = startType(args);
    return switch (startType) {
      case BROKER -> broker(args);
      case GATEWAY -> gateway(args);
    };
  }

  /**
   * 按 Spring 的优先级顺序解析启动类型：命令行参数 > 系统属性 > 环境变量。
   *
   * <p>支持 --start.type=gateway、-Dstart.type=gateway、环境变量 START_TYPE=gateway
   */
  private static StartType startType(final String[] args) {
    final StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource(args));
    return Binder.get(environment)
        .bind("start.type", Bindable.of(StartType.class))
        .orElse(StartType.BROKER);
  }

  /** start */
  public void run() {
    application.run();
  }

  /** 仅broker */
  private static SpringApplication broker(final String[] args) {
    return createDefaultApplicationBuilder()
        .sources(BrokerModuleConfiguration.class)
        .profiles(ProfileType.BROKER.getId(), ProfileType.STANDALONE.getId())
        .initializers(new BrokerHealthConfigurationInitializer())
        .build(args);
  }

  /** 仅gateway */
  private static SpringApplication gateway(final String[] args) {
    return createDefaultApplicationBuilder()
        .sources(GatewayModuleConfiguration.class)
        .profiles(ProfileType.GATEWAY.getId(), ProfileType.STANDALONE.getId())
        .initializers(new GatewayHealthConfigurationInitializer())
        .build(args);
  }

  /** 设置默认全局配置 */
  private static void setDefaultGlobalConfiguration(final String[] args) {
    final String startTypeName = startType(args).getId();
    putSystemPropertyIfAbsent("logging.file.name", "kunpeng-" + startTypeName);
    putSystemPropertyIfAbsent("application.name", "kunpeng-" + startTypeName);
    Thread.setDefaultUncaughtExceptionHandler(FatalErrorHandler.uncaughtExceptionHandler(LOGGER));
    putSystemPropertyIfAbsent("io.grpc.netty.useCustomAllocator", Boolean.toString(false));
    putSystemPropertyIfAbsent(
        "spring.banner.location", "classpath:/banners/" + startTypeName + ".txt");
  }

  /** 创建默认应用程序构建器 */
  private static SpringApplicationBuilder createDefaultApplicationBuilder() {
    return new SpringApplicationBuilder().web(WebApplicationType.REACTIVE).logStartupInfo(true);
  }

  /** 设置系统参数 */
  private static void putSystemPropertyIfAbsent(final String key, final String value) {
    if (System.getProperty(key) == null) {
      System.setProperty(key, value);
    }
  }
}

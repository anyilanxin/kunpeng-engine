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

package com.anyilanxin.kunpeng.bpm.parse.dmn.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * 实现日志记录器类的基类。日志记录器类是一种为每条日志消息提供专用方法的类：
 *
 * <pre>
 * public class MyLogger extends BaseLogger {
 *
 *   public static MyLogger LOG = createLogger(MyLogger.class, "MYPROJ", "org.example", "01");
 *
 *   public void engineStarted(long currentTime) {
 *     logInfo("100", "My super engine has started at '{}'", currentTime);
 *   }
 *
 * }
 * </pre>
 *
 * <p>然后可以通过以下方式使用该日志记录器：
 *
 * <pre>
 * LOG.engineStarted(System.currentTimeMilliseconds());
 * </pre>
 *
 * <p>这将输出以下消息：
 *
 * <pre>
 * INFO  org.example - MYPROJ-01100 My super engine has started at '4234234523'
 * </pre>
 *
 * <h2>Slf4j</h2>
 *
 * 该类使用 slf4j 作为日志 API，并确保日志消息与异常消息始终使用相同的模板进行格式化。
 *
 * <h2>日志消息格式</h2>
 *
 * 该类生成的日志消息格式如下：
 *
 * <pre>
 * [PROJECT_CODE]-[COMPONENT_ID][MESSAGE_ID] message
 * </pre>
 *
 * 示例：
 *
 * <pre>
 * MYPROJ-01100 My super engine has started at '4234234523'
 * </pre>
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public abstract class BaseLogger {

  /** 委托的 slf4j 日志记录器 */
  protected Logger delegateLogger;

  /** 日志记录器的项目代码 */
  protected String projectCode;

  /** 日志记录器的组件 ID。 */
  protected String componentId;

  protected BaseLogger() {}

  /**
   * 创建一个新的 {@link BaseLogger Logger} 实例。
   *
   * @param loggerClass 日志记录器的类型
   * @param projectCode 完整项目的唯一代码。
   * @param name 要使用的 slf4j 日志记录器名称。
   * @param componentId 组件的唯一 ID。
   */
  public static <T extends BaseLogger> T createLogger(
      final Class<T> loggerClass,
      final String projectCode,
      final String name,
      final String componentId) {
    try {
      final T logger = loggerClass.newInstance();
      logger.projectCode = projectCode;
      logger.componentId = componentId;
      logger.delegateLogger = LoggerFactory.getLogger(name);

      return logger;

    } catch (final InstantiationException e) {
      throw new RuntimeException("Unable to instantiate logger '" + loggerClass.getName() + "'", e);

    } catch (final IllegalAccessException e) {
      throw new RuntimeException("Unable to instantiate logger '" + loggerClass.getName() + "'", e);
    }
  }

  /**
   * 以指定的日志级别记录消息。若无法匹配该日志级别，则默认使用 DEBUG。
   *
   * @param level 日志级别
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void log(
      final String level,
      final String id,
      final String messageTemplate,
      final Object... parameters) {
    log(level, Level.DEBUG, id, messageTemplate, parameters);
  }

  /**
   * 以指定的日志级别记录消息。
   *
   * @param level 日志级别
   * @param defaultLevel 无法匹配日志级别时使用的默认日志级别
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void log(
      final String level,
      final Level defaultLevel,
      final String id,
      final String messageTemplate,
      final Object... parameters) {
    switch (Level.parse(level, defaultLevel)) {
      case ERROR:
        logError(id, messageTemplate, parameters);
        break;
      case WARN:
        logWarn(id, messageTemplate, parameters);
        break;
      case INFO:
        logInfo(id, messageTemplate, parameters);
        break;
      case DEBUG:
        logDebug(id, messageTemplate, parameters);
        break;
      case TRACE:
        logTrace(id, messageTemplate, parameters);
        break;
    }
  }

  /**
   * 记录一条 'TRACE' 消息
   *
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void logTrace(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isTraceEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.trace(msg, parameters);
    }
  }

  /**
   * 记录一条 'DEBUG' 消息
   *
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void logDebug(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isDebugEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.debug(msg, parameters);
    }
  }

  /**
   * 记录一条 'INFO' 消息
   *
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void logInfo(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isInfoEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.info(msg, parameters);
    }
  }

  /**
   * 记录一条 'WARN' 消息
   *
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void logWarn(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isWarnEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.warn(msg, parameters);
    }
  }

  /**
   * 记录一条 'ERROR' 消息
   *
   * @param id 此日志消息的唯一 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 可选参数列表
   */
  protected void logError(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isErrorEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.error(msg, parameters);
    }
  }

  /**
   * @return 若该日志记录器会记录 'DEBUG' 消息则为 true
   */
  public boolean isDebugEnabled() {
    return delegateLogger.isDebugEnabled();
  }

  /**
   * @return 若该日志记录器会记录 'INFO' 消息则为 true
   */
  public boolean isInfoEnabled() {
    return delegateLogger.isInfoEnabled();
  }

  /**
   * @return 若该日志记录器会记录 'WARN' 消息则为 true
   */
  public boolean isWarnEnabled() {
    return delegateLogger.isWarnEnabled();
  }

  /**
   * @return 若该日志记录器会记录 'ERROR' 消息则为 true
   */
  public boolean isErrorEnabled() {
    return delegateLogger.isErrorEnabled();
  }

  /**
   * 格式化消息模板
   *
   * @param id 消息的 ID
   * @param messageTemplate 要使用的消息模板
   * @return 格式化后的模板
   */
  protected String formatMessageTemplate(final String id, final String messageTemplate) {
    return projectCode + "-" + componentId + id + " " + messageTemplate;
  }

  /**
   * 准备异常消息
   *
   * @param id 消息的 ID
   * @param messageTemplate 要使用的消息模板
   * @param parameters 消息的参数（可选）
   * @return 准备好的异常消息
   */
  protected String exceptionMessage(
      final String id, final String messageTemplate, final Object... parameters) {
    final String formattedTemplate = formatMessageTemplate(id, messageTemplate);
    if (parameters == null || parameters.length == 0) {
      return formattedTemplate;

    } else {
      return MessageFormatter.arrayFormat(formattedTemplate, parameters).getMessage();
    }
  }
}

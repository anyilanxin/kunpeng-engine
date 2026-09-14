/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyilanxin.kunpeng.engine.dmn.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * Base class for implementing a logger class. A logger class is a class with dedicated methods for
 * each log message:
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
 * <p>The logger can then be used in the following way:
 *
 * <pre>
 * LOG.engineStarted(System.currentTimeMilliseconds());
 * </pre>
 *
 * <p>This will print the following message:
 *
 * <pre>
 * INFO  org.example - MYPROJ-01100 My super engine has started at '4234234523'
 * </pre>
 *
 * <h2>Slf4j</h2>
 *
 * This class uses slf4j as logging API. The class ensures that log messages and exception messages
 * are always formatted using the same template.
 *
 * <h2>Log message format</h2>
 *
 * The log message format produced by this class is as follows:
 *
 * <pre>
 * [PROJECT_CODE]-[COMPONENT_ID][MESSAGE_ID] message
 * </pre>
 *
 * Example:
 *
 * <pre>
 * MYPROJ-01100 My super engine has started at '4234234523'
 * </pre>
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public abstract class BaseLogger {

  /** the slf4j logger we delegate to */
  protected Logger delegateLogger;

  /** the project code of the logger */
  protected String projectCode;

  /** the component Id of the logger. */
  protected String componentId;

  protected BaseLogger() {}

  /**
   * Creates a new instance of the {@link BaseLogger Logger}.
   *
   * @param loggerClass the type of the logger
   * @param projectCode the unique code for a complete project.
   * @param name the name of the slf4j logger to use.
   * @param componentId the unique id of the component.
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
   * Logs a message with the specified log level. If the log level cannot be matched, it defaults to
   * DEBUG.
   *
   * @param level the log level
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void log(
      final String level,
      final String id,
      final String messageTemplate,
      final Object... parameters) {
    log(level, Level.DEBUG, id, messageTemplate, parameters);
  }

  /**
   * Logs a message with the specified log level.
   *
   * @param level the log level
   * @param defaultLevel the default log level to use when log level cannot be matched
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
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
   * Logs a 'TRACE' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logTrace(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isTraceEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.trace(msg, parameters);
    }
  }

  /**
   * Logs a 'DEBUG' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logDebug(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isDebugEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.debug(msg, parameters);
    }
  }

  /**
   * Logs an 'INFO' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logInfo(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isInfoEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.info(msg, parameters);
    }
  }

  /**
   * Logs an 'WARN' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logWarn(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isWarnEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.warn(msg, parameters);
    }
  }

  /**
   * Logs an 'ERROR' message
   *
   * @param id the unique id of this log message
   * @param messageTemplate the message template to use
   * @param parameters a list of optional parameters
   */
  protected void logError(
      final String id, final String messageTemplate, final Object... parameters) {
    if (delegateLogger.isErrorEnabled()) {
      final String msg = formatMessageTemplate(id, messageTemplate);
      delegateLogger.error(msg, parameters);
    }
  }

  /**
   * @return true if the logger will log 'DEBUG' messages
   */
  public boolean isDebugEnabled() {
    return delegateLogger.isDebugEnabled();
  }

  /**
   * @return true if the logger will log 'INFO' messages
   */
  public boolean isInfoEnabled() {
    return delegateLogger.isInfoEnabled();
  }

  /**
   * @return true if the logger will log 'WARN' messages
   */
  public boolean isWarnEnabled() {
    return delegateLogger.isWarnEnabled();
  }

  /**
   * @return true if the logger will log 'ERROR' messages
   */
  public boolean isErrorEnabled() {
    return delegateLogger.isErrorEnabled();
  }

  /**
   * Formats a message template
   *
   * @param id the id of the message
   * @param messageTemplate the message template to use
   * @return the formatted template
   */
  protected String formatMessageTemplate(final String id, final String messageTemplate) {
    return projectCode + "-" + componentId + id + " " + messageTemplate;
  }

  /**
   * Prepares an exception message
   *
   * @param id the id of the message
   * @param messageTemplate the message template to use
   * @param parameters the parameters for the message (optional)
   * @return the prepared exception message
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

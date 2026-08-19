/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.script.impl;

import com.anyilanxin.kunpeng.engine.script.EvaluationResult;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.apache.commons.lang3.math.NumberUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @author zxuanhong
 * @date 2025-10-10 23:52
 * @since
 */
public final class EvaluationResultImpl implements EvaluationResult {
  private final String expression;
  private boolean failure;
  private String failureMessage;
  private Object result;
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
  private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  public EvaluationResultImpl(final String expression) {
    this.expression = expression;
  }

  @Override
  public String getExpression() {
    return expression;
  }

  @Override
  public boolean isFailure() {
    return failure;
  }

  @Override
  public String getFailureMessage() {
    return failureMessage;
  }

  @Override
  public Object getObject() {
    return result;
  }

  @Override
  public String getString() {
    if (result instanceof final String string) {
      return string;
    }
    return result != null ? result.toString() : "";
  }

  @Override
  public Boolean getBoolean() {
    if (result instanceof final Boolean booleanValue) {
      return booleanValue;
    } else if (result instanceof final String booleanStr) {
      try {
        return Boolean.parseBoolean(booleanStr);
      } catch (final Exception e) {
        failureMessage = e.getMessage();
        failure = true;
      }
    }
    return null;
  }

  @Override
  public Number getNumber() {
    if (result instanceof final Number number) {
      return number;
    } else if (result instanceof final String numberStr) {
      try {
        return NumberUtils.createNumber(numberStr);
      } catch (final Exception e) {
        failureMessage = e.getMessage();
        failure = true;
      }
    }
    return null;
  }

  @Override
  public Duration getDuration() {
    if (result instanceof final Duration duration) {
      return duration;
    } else if (result instanceof final String durationStr) {
      try {
        if (durationStr.startsWith("PT")) {
          return Duration.parse(durationStr);
        } else {
          throw new IllegalAccessException("无法转换为Duration");
        }
      } catch (final Exception e) {
        failureMessage = e.getMessage();
        failure = true;
      }
    }
    return null;
  }

  @Override
  public Period getPeriod() {
    if (result instanceof final Period period) {
      return period;
    } else if (result instanceof final String periodStr) {
      try {
        if (periodStr.startsWith("P") && !periodStr.contains("T")) {
          return Period.parse(periodStr);
        } else {
          throw new IllegalAccessException("无法转换为Period");
        }
      } catch (final Exception e) {
        failureMessage = e.getMessage();
        failure = true;
      }
    } else {
      failureMessage = "无法转换为Period";
      failure = true;
    }
    return null;
  }

  @Override
  public ZonedDateTime getDateTime() {
    switch (result) {
      case final ZonedDateTime zonedDateTime -> {
        return zonedDateTime;
      }
      case final String zonedDateTimeStr -> {
        try {
          return ZonedDateTime.parse(zonedDateTimeStr, FORMATTER);
        } catch (final DateTimeParseException e) {
          failureMessage = e.getMessage();
          failure = true;
        }
      }
      case final LocalDateTime localDateTime -> {
        return localDateTime.atZone(ZoneId.systemDefault());
      }
      case null, default -> {
        failureMessage = "无法转换为ZonedDateTime";
        failure = true;
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings({"rawtypes"})
  public List<String> getListOfStrings() {
    if (result instanceof final List listOfObject) {
      final List<String> list = new ArrayList<>();
      for (final Object object : listOfObject) {
        if (object != null) {
          list.add(object.toString());
        } else {
          list.add(null);
        }
      }
      return list;
    } else if (result instanceof final String listStr) {
      try {
        return MAPPER.convertValue(listStr, LIST_TYPE);
      } catch (final Exception e) {
        failureMessage = "无法转换为List<String>";
        failure = true;
      }
    } else {
      failureMessage = "无法转换为List<String>";
      failure = true;
    }
    return null;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Map<String, Object> getMapOfObject() {
    if (result instanceof final Map mapOfObject) {
      final Map<String, Object> mapObject = new HashMap<>();
      final Map<Object, Object> map = (Map<Object, Object>) mapOfObject;
      for (final Map.Entry<Object, Object> objectObjectEntry : map.entrySet()) {
        mapObject.put(objectObjectEntry.getKey().toString(), objectObjectEntry.getValue());
      }
      return mapObject;
    } else if (result instanceof final String listStr) {
      try {
        return MAPPER.convertValue(listStr, MAP_TYPE);
      } catch (final Exception e) {
        failureMessage = "无法转换为Map<String, Object>";
        failure = true;
      }
    } else {
      failureMessage = "无法转换为Map<String, Object>";
      failure = true;
    }
    return null;
  }

  public void setFailure(final boolean failure) {
    this.failure = failure;
  }

  public void setFailureMessage(final String failureMessage) {
    this.failureMessage = failureMessage;
  }

  public void setResult(final Object result) {
    this.result = result;
  }
}

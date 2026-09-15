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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation;

import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.CronTimer;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.Interval;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.RepeatingInterval;
import com.anyilanxin.kunpeng.bpm.model.bpmn.util.time.TimeDateTimer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.utils.Either;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/** 转换辅助工具：把定时事件的三种表达式（时长/周期/时间点）编译为延迟到运行期执行的定时构建工厂。 */
public final class TransformHelper {

  private TransformHelper() {}

  /**
   * 构建时长定时（timeDuration）：求值结果支持 Duration、Period、Interval 或 ISO-8601 字符串，统一转为单次重复的重复间隔。
   *
   * @param expression 已解析的时长表达式
   * @param content 原始表达式文本
   * @return 装配完成的定时载荷
   */
  public static BpmnCatchEventElement.TimerProperties durationTimer(
      final ScriptExpression expression, final String content) {
    return new BpmnCatchEventElement.TimerProperties()
        .setTimerType(BpmnCatchEventElement.TimerProperties.TimerType.DURATION)
        .setTimerContent(content)
        .setTimerFactory(
            scriptContext -> {
              final Either<String, Object> result = expression.evaluateObject(scriptContext);
              if (result.isLeft()) {
                return Either.left(result.getLeft());
              }
              final Interval interval = toInterval(result.get());
              if (interval == null) {
                return Either.left("Unsupported timer type: " + content);
              }
              return Either.right(new RepeatingInterval(content, 1, interval));
            });
  }

  /**
   * 构建周期定时（timeCycle）：求值结果为 ISO 重复间隔（R 开头）或 cron 表达式。
   *
   * @param expression 已解析的周期表达式
   * @param content 原始表达式文本
   * @return 装配完成的定时载荷
   */
  public static BpmnCatchEventElement.TimerProperties cycleTimer(
      final ScriptExpression expression, final String content) {
    return new BpmnCatchEventElement.TimerProperties()
        .setTimerType(BpmnCatchEventElement.TimerProperties.TimerType.CYCLE)
        .setTimerContent(content)
        .setTimerFactory(
            scriptContext -> {
              final Either<String, String> result = expression.evaluateString(scriptContext);
              if (result.isLeft()) {
                return Either.left(result.getLeft());
              }
              final String text = result.get();
              if (text.startsWith("R")) {
                return Either.right(RepeatingInterval.parse(text));
              }
              try {
                return Either.right(CronTimer.parse(text));
              } catch (final DateTimeParseException e) {
                return Either.left(e.getMessage());
              }
            });
  }

  /**
   * 构建时间点定时（timeDate）：求值结果为带时区的时间点。
   *
   * @param expression 已解析的时间点表达式
   * @param content 原始表达式文本
   * @return 装配完成的定时载荷
   */
  public static BpmnCatchEventElement.TimerProperties dateTimer(
      final ScriptExpression expression, final String content) {
    return new BpmnCatchEventElement.TimerProperties()
        .setTimerType(BpmnCatchEventElement.TimerProperties.TimerType.DATE)
        .setTimerContent(content)
        .setTimerFactory(
            scriptContext -> {
              final Either<String, ZonedDateTime> result =
                  expression.evaluateDateTime(scriptContext);
              if (result.isLeft()) {
                return Either.left(result.getLeft());
              }
              return Either.right(new TimeDateTimer(result.get()));
            });
  }

  /** 把求值结果归一为间隔，无法识别时返回 null。 */
  private static Interval toInterval(final Object value) {
    if (value instanceof final Duration duration) {
      return new Interval(duration);
    }
    if (value instanceof final Period period) {
      return new Interval(period);
    }
    if (value instanceof final Interval interval) {
      return interval;
    }
    if (value instanceof final String iso) {
      if (iso.startsWith("PT")) {
        return new Interval(Duration.parse(iso));
      }
      if (iso.startsWith("P")) {
        return iso.contains("T") ? Interval.parse(iso) : new Interval(Period.parse(iso));
      }
    }
    return null;
  }
}

/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.bpm.model.bpmn.util.time;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class CronTimer implements Timer {

  private final Cron cron;

  private int repetitions;

  private final String content;

  public CronTimer(final Cron cron) {
    this.cron = cron;
    content = cron.asString();
  }

  @Override
  public Interval getInterval() {
    return null;
  }

  @Override
  public int getRepetitions() {
    return repetitions;
  }

  @Override
  public long getDueDate(final long fromEpochMilli) {
    // set default value to -1
    repetitions = -1;
    final var next =
        ExecutionTime.forCron(cron)
            .nextExecution(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(fromEpochMilli), ZoneId.systemDefault()))
            .map(ZonedDateTime::toInstant)
            .map(Instant::toEpochMilli);

    // set `repetitions` to 0 when the next execution time does not exist
    if (next.isEmpty()) {
      repetitions = 0;
    }

    return next.orElse(fromEpochMilli);
  }

  public static CronTimer parse(final String text) {
    try {
      final var cron =
          new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING53))
              .parse(text);
      return new CronTimer(cron);
    } catch (final IllegalArgumentException | NullPointerException ex) {
      throw new DateTimeParseException(ex.getMessage(), Objects.requireNonNullElse(text, ""), 0);
    }
  }

  @Override
  public String getContent() {
    return content;
  }
}
